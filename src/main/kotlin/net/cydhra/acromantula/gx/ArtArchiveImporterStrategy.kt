package net.cydhra.acromantula.gx

import kotlinx.coroutines.CompletableJob
import net.cydhra.acromantula.features.archives.ArchiveFeature
import net.cydhra.acromantula.features.importer.ImporterFeature
import net.cydhra.acromantula.features.importer.ImporterStrategy
import net.cydhra.acromantula.workspace.filesystem.FileEntity
import java.io.ByteArrayInputStream
import java.io.PushbackInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log

/**
 * Import .art banks of CT3. They contain a set of GXT(X) files, which are GT Texture files.
 */
class ArtArchiveImporterStrategy : ImporterStrategy {

    override suspend fun handles(fileName: String, fileContent: PushbackInputStream): Boolean {
        // .art does not seem to have magic bytes. How dare they?
        return fileName.endsWith(".art")
    }

    @Suppress("UsePropertyAccessSyntax") // buffer accessors (`getInt` etc) are modifying the underlying buffer,
    // contrary to a getter contract
    override suspend fun import(
        supervisor: CompletableJob,
        parent: FileEntity?,
        fileName: String,
        fileContent: PushbackInputStream
    ): Pair<FileEntity, ByteArray?> {
        val byteArray = fileContent.readBytes()
        val buffer = ByteBuffer
            .wrap(byteArray)
            .order(ByteOrder.LITTLE_ENDIAN)

        // add archive directory for the bank
        val archiveDirectory = ArchiveFeature.addDirectory(fileName, parent)

        val imageNumber = buffer.getInt()
        val offsetTable = IntArray(imageNumber + 1) { if (it < imageNumber) buffer.getInt() else byteArray.size }
        val decimals = log(imageNumber.toDouble(), 10.0).toInt()

        for (index in (0 until imageNumber)) {
            ImporterFeature.importFile(
                supervisor,
                archiveDirectory,
                "image${String.format("%0${decimals}d", index)}",
                ByteArrayInputStream(
                    byteArray,
                    offsetTable[index],
                    offsetTable[index + 1] - offsetTable[index]
                )
            )
        }

        ArchiveFeature.markDirectoryAsArchive(archiveDirectory, ArtArchiveType)

        return Pair(archiveDirectory, null)
    }
}