import java.util.Locale

plugins {
    java
    application
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "CircuitSim"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.ra4king.circuitsim.EntrypointKt" // Match application.mainClass
    }

    // Include dependencies in the fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

application {
    mainClass.set("com.ra4king.circuitsim.EntrypointKt")
}
kotlin {
    jvmToolchain( 17 )
}

val javaFxVersion = "22"

dependencies {
    implementation("com.google.code.gson:gson:2.10")
    val javafxModules = arrayOf("base", "controls", "graphics", "fxml", "swing")
    javafxModules.forEach { module ->
        implementation ("org.openjfx:javafx-${module}:${javaFxVersion}:${getOs()}")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}

fun getOs(): String {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
    if (osName.contains("win")) return "win"
    if (osName.contains("mac")) return "mac"
    if (osName.contains("linux")) return "linux"
    throw GradleException("Unsupported OS: $osName")
}