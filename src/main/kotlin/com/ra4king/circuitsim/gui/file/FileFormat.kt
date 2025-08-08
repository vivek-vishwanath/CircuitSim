package com.ra4king.circuitsim.gui.file

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ra4king.circuitsim.gui.CircuitSim
import com.ra4king.circuitsim.gui.Properties
import java.io.*
import java.security.MessageDigest
import java.util.*

/**
 * @author Roi Atalla
 */
object FileFormat {
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    @Throws(IOException::class)
    fun readFile(reader: Reader): String {
        val string = StringBuilder()
        BufferedReader(reader).use { bufReader ->
            var line: String?
            while ((bufReader.readLine().also { line = it }) != null) {
                string.append(line).append("\n")
            }
            return string.toString()
        }
    }

    @Throws(IOException::class)
    fun readFile(file: File) = readFile(FileReader(file))

    @Throws(IOException::class)
    fun writeFile(file: File, contents: String) {
        FileWriter(file).use { writer ->
            writer.write(contents)
            writer.write('\n'.code)
        }
    }

    private fun sha256ify(input: String): String {
        // Shamelessly stolen from:
        // https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            //Applies sha256 to our input,
            val hash = digest.digest(input.toByteArray(charset("UTF-8")))
            val hexString = StringBuffer() // This will contain hash as hexidecimal
            for (i in hash.indices) {
                val hex = Integer.toHexString(0xff and hash[i].toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            return hexString.toString()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getLastHash(revisionSignatures: MutableList<String>) =
        if (revisionSignatures.isEmpty()) ""
        else RevisionSignatureBlock(revisionSignatures[revisionSignatures.size - 1]).currentHash

    @JvmStatic
    @Throws(IOException::class)
    fun save(file: File, circuitFile: CircuitFile) {
        circuitFile.addRevisionSignatureBlock()
        writeFile(file, stringify(circuitFile))
    }

    @JvmStatic
    fun stringify(circuitFile: CircuitFile): String = GSON.toJson(circuitFile)

    @JvmStatic
    @Throws(IOException::class)
    fun load(file: File?, taDebugMode: Boolean): CircuitFile {
        val savedFile = file?.let { parse(readFile(it)) } ?: throw NullPointerException("File is empty!")
        if (!taDebugMode && !savedFile.revisionSignaturesAreValid())
            throw NullPointerException("File is corrupted. Contact Course Staff for Assistance.")
        return savedFile
    }

    @JvmStatic
    fun parse(contents: String?): CircuitFile? = GSON.fromJson(contents, CircuitFile::class.java)

    class RevisionSignatureBlock private constructor(
        currentHash: String?,
        val previousHash: String, val fileDataHash: String,
        val timeStamp: String, val copiedBlocks: String
    ) {

        val currentHash = currentHash ?: hash()

        constructor(previousHash: String, fileDataHash: String, copiedBlocks: MutableList<String>) :
                this(
                    null, previousHash, fileDataHash,
                    System.currentTimeMillis().toString(),
                    copiedBlocks.joinToString("") { "\t$it" })

        constructor(stringifiedBlock: String) : this(new(stringifiedBlock))

        private constructor(strings: Array<String>) : this(strings[0], strings[1], strings[2], strings[3], strings[4])

        companion object {
            private fun new(stringifiedBlock: String): Array<String> {
                val decodedBlock = String(Base64.getDecoder().decode(stringifiedBlock.toByteArray()))
                val fields = decodedBlock.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val copiedBlocks = when {
                    fields.size < 4 -> throw NullPointerException("File is corrupted. Contact Course Staff for Assistance.")
                    fields.size == 4 -> ""
                    else -> fields.copyOfRange(4, fields.size).joinToString("") { "\t$it" }
                }
                return arrayOf(fields[1], fields[0], fields[3], fields[2], copiedBlocks)
            }
        }

        fun hash() = sha256ify(previousHash + fileDataHash + timeStamp + this.copiedBlocks)

        fun stringify(): String =
            // Lack of a tab between fileDataHash and copiedBlocks is intentional. copiedBlocks starts with a tab.
            Base64.getEncoder().encodeToString(
                String.format(
                    "%s\t%s\t%s\t%s%s",
                    previousHash, currentHash, timeStamp, fileDataHash, copiedBlocks
                ).toByteArray()
            )
    }

    class CircuitFile(
        val version: String, val globalBitSize: Int, val clockSpeed: Int,
        val libraryPaths: MutableSet<String>?, val circuits: MutableList<CircuitInfo>,
        val revisionSignatures: MutableList<String>, var copiedBlocks: MutableList<String>?
    ) {
        private fun hash() = sha256ify((GSON.toJson(libraryPaths) + GSON.toJson(circuits)))

        fun addRevisionSignatureBlock() {
            val currentFileDataHash = hash()
            val previousHash = getLastHash(revisionSignatures)
            val newBlock = RevisionSignatureBlock(previousHash, currentFileDataHash, copiedBlocks!!)
            revisionSignatures.add(newBlock.stringify())
            this.copiedBlocks = null
        }

        fun revisionSignaturesAreValid(): Boolean {
            if (revisionSignatures.isEmpty()) return false
            val expectedFileDataHash = this.hash()
            val lastBlock =
                RevisionSignatureBlock(this.revisionSignatures[this.revisionSignatures.size - 1])
            val actualFileDataHash = lastBlock.fileDataHash
            if (actualFileDataHash != expectedFileDataHash || lastBlock.currentHash != lastBlock.hash()) {
                return false
            }
            val blocks = this.revisionSignatures.toTypedArray()
            for (i in blocks.size - 1 downTo 1) {
                val block = RevisionSignatureBlock(blocks[i])
                val prevBlock = RevisionSignatureBlock(blocks[i - 1])
                if (block.currentHash != block.hash() || block.previousHash != prevBlock.currentHash) {
                    return false
                }
            }
            return RevisionSignatureBlock(blocks[0]).previousHash == ""
        }

        constructor(
            globalBitSize: Int, clockSpeed: Int, libraryPaths: MutableSet<String>?, circuits: MutableList<CircuitInfo>,
            revisionSignatures: MutableList<String>, copiedBlocks: MutableList<String>
        ) : this(
            CircuitSim.VERSION, globalBitSize, clockSpeed,
            libraryPaths, circuits, revisionSignatures, copiedBlocks
        )
    }

    class CircuitInfo(val name: String, val components: List<ComponentInfo>, val wires: List<WireInfo>)

    class ComponentInfo internal constructor(
        val name: String,
        val x: Int,
        val y: Int,
        val properties: MutableMap<String, String>
    ) {
        constructor(name: String, x: Int, y: Int, properties: Properties) : this(name, x, y, HashMap()) {
            properties.forEach { prop: Properties.Property<*> ->
                if (!prop.ephemeral) {
                    this.properties.put(prop.name, prop.stringValue)
                }
            }
        }

        override fun hashCode() = Objects.hash(name, x, y, properties)

        override fun equals(other: Any?) = other is ComponentInfo && name == other.name &&
                x == other.x && y == other.y && properties == other.properties
    }

    class WireInfo(val x: Int, val y: Int, val length: Int, val isHorizontal: Boolean) {
        override fun hashCode() = Objects.hash(x, y, length, isHorizontal)

        override fun equals(other: Any?) = other is WireInfo && x == other.x && y == other.y &&
                length == other.length && isHorizontal == other.isHorizontal
    }
}
