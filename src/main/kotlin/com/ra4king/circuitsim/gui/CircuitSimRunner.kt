package com.ra4king.circuitsim.gui

import com.ra4king.circuitsim.gui.CircuitSim.Companion.run
import javafx.application.Platform
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Roi Atalla
 */
object CircuitSimRunner {

    @JvmStatic
    fun main(args: Array<String>) {
        NativeLibraryExtractor().use { extractor ->
            extractor.extractNativeLibs()
            run(args)
            Platform.exit()
        }
    }

    /**
     * Hack to make the CircuitSim fat jar work on both amd64 and aarch64.
     *
     *
     * Naively supporting both amd64 and aarch64 Macs (for example) in one fat
     * jar would require bundling JavaFX native libraries for both amd64 and
     * aarch64 in our fat jar... with the same filename. The reason for this is
     * that the
     * [
 * JavaFX code for loading the native libraries from the running jar](https://github.com/openjdk/jfx/blob/86b854dc367fb32743810716da5583f7d59208f8/modules/javafx.graphics/src/main/java/com/sun/glass/utils/NativeLibLoader.java#L201) is
     * quite inflexible and simply attempts to load e.g. libjavafx_font.so with
     * no consideration for which architecture is running. We avoid this first
     * stumbling block by having the Gradle build script sort the native
     * libraries into architecture-specific directories inside the CircuitSim
     * fat jar.
     *
     *
     * The next hurdle is getting JavaFX to load the native libraries for the
     * current architecture. Thankfully, when JavaFX cannot find native
     * libraries at the root of the running jar,
     * [
 * JavaFX manually queries java.library.path](https://github.com/openjdk/jfx/blob/86b854dc367fb32743810716da5583f7d59208f8/modules/javafx.graphics/src/main/java/com/sun/glass/utils/NativeLibLoader.java#L143) and walks through it,
     * looking for the native libraries. (If it simply called
     * System.loadLibrary(),
     * [we could not
 * modify java.library.path.](https://stackoverflow.com/a/10144117/321301)) So our job here is to create a temporary
     * directory, fill it with the native libraries for this OS and
     * architecture, and put that temporary directory in java.library.path.
     * Then JavaFX will load the native libraries! (Our job is also to delete
     * the temporary directory when CircuitSim exits.)
     *
     *
     * Make this class public in case some dependency like the autograder
     * library needs to use it.
     */
    class NativeLibraryExtractor : AutoCloseable {
        private var tempDir: Path? = null

        fun extractNativeLibs() {
            val nativeLibraryExtension = guessNativeLibraryExtension()

            // Skip this whole process on Windows and let JavaFX grab the
            // .dlls from the root of the jar. Why do this? Well, we only
            // support amd64 on Windows anyway. But more importantly,
            // there is not a good way for us to clean up the temporary
            // .dlls after ourselves on Windows. That's because the JRE
            // keeps JNI libraries open until the ClassLoader under which
            // they were System.load()ed is garbage collected. So until
            // then, we cannot remove the .dlls, since Windows keeps them
            // locked on-disk. But the only problem is that the
            // ClassLoader will never be garbage collected until the JVM
            // exits entirely, since we are using the built-in
            // ClassLoader! So admit defeat to avoid filling up students'
            // hard drives with .dlls and showing them confusing error
            // messages.
            if (nativeLibraryExtension == ".dll") return

            val arch = guessArchitecture()

            val archDirPathName = "/$arch"
            val archDirResource = NativeLibraryExtractor::class.java.getResource(archDirPathName)

            if (archDirResource == null) {
                // Note that ./gradlew run doesn't perform the same copying process that
                // ./gradlew jar does.
                //
                // So, if no arch folders exist, we should ignore and move on,
                // so ./gradlew run can continue smoothly.
                //
                // This technically can cause an issue if someone tries to run a JAR on an unsupported architecture,
                // but they will just get a slightly worse error (and will still see this message).
                System.err.println("Can't find native libraries for architecture $arch")
                return
            }

            try {
                tempDir = Files.createTempDirectory("circuitsim-libs")
            } catch (exc: IOException) {
                tempDir = null
                throw RuntimeException("Couldn't create temporary directory for native libraries", exc)
            }

            val archDir = try {
                archDirResource.toURI()
            } catch (exc: URISyntaxException) {
                // Checked exception
                throw RuntimeException(exc)
            }

            try {
                FileSystems.newFileSystem(archDir, mutableMapOf<String, String>()).use { fs ->
                    val dir = fs.getPath(archDirPathName)
                    Files.list(dir).forEach { nativeLib: Path ->
                        // Performance optimization: we are on the critical path
                        // here, so avoid copying libraries we don't need
                        val baseName = nativeLib.fileName.toString()
                        if (baseName.endsWith(nativeLibraryExtension)) {
                            val dest = tempDir!!.resolve(nativeLib.fileName.toString())
                            try {
                                Files.copy(nativeLib, dest)
                            } catch (e: IOException) {
                                throw RuntimeException("Could not copy native library from jar to disk", e)
                            }
                        }
                    }
                }
            } catch (exc: IOException) {
                throw RuntimeException("Could not copy native libraries from jar to disk", exc)
            }

            val existingNativeLibPath = System.getProperty("java.library.path")
            if (existingNativeLibPath == null) {
                System.setProperty("java.library.path", tempDir.toString())
            } else {
                System.setProperty(
                    "java.library.path",
                    existingNativeLibPath + File.pathSeparatorChar + tempDir.toString()
                )
            }
        }

        override fun close() {
            // No temporary directory created (we might be on Windows),
            // so nothing to do here
            if (tempDir == null) return

            var success = true

            // When we catch IOExceptions here, print the errors instead of
            // re-throwing. The idea is that even if one deletion fails, we
            // want to try and delete as many of the rest as we can
            val children = try {
                Files.list(tempDir)
            } catch (_: IOException) {
                success = false
                null
            }

            children?.use { stream ->
                stream.forEach { child ->
                    try {
                        Files.delete(child)
                    } catch (_: IOException) {
                        success = false
                    }
                }
            }

            try {
                Files.delete(tempDir!!)
            } catch (_: IOException) {
                success = false
            }

            if (!success) {
                System.err.println("Warning: Could not delete some temporarily-extracted native JavaFX libraries. If you care, the following directory is now wasting your disk space: $tempDir")
            }
        }

        companion object {
            // Return .dll on Windows, .so on Linux, .dylib on macOS, etc.
            private fun guessNativeLibraryExtension(): String {
                val fooDotDll = System.mapLibraryName("foo")
                fooDotDll?.let {
                    val dotIdx = it.lastIndexOf('.')
                    if (dotIdx != -1) return it.substring(dotIdx)
                }
                throw RuntimeException("Unsupported format of native library filenames. Bug in JRE?")
            }

            private fun guessArchitecture(): String {
                val arch = System.getProperty("os.arch")
                    ?: throw RuntimeException("JRE did not give us an architecture, no way to load native libraries")
                // Handle special case of amd64 being named x86_64 on Macs:
                // https://github.com/openjdk/jdk/blob/9def4538ab5456d689fd289bdef66fd1655773bc/make/autoconf/platform.m4#L480
                // This appears to have been done for backwards compatibilty with
                // Apple's JRE:
                // https://mail.openjdk.org/pipermail/macosx-port-dev/2012-February/002850.html
                // But our Gradle build script will place all x86_64/amd64 binaries
                // in a directory in the jar named "amd64", not "x86_64", so we
                // need to return that name instead
                return if(arch == "x86_64") "amd64" else arch
            }
        }
    }
}
