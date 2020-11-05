package net.cydhra.acromantula.gx

import net.cydhra.acromantula.features.importer.ImporterFeature
import net.cydhra.acromantula.features.importer.ImporterStrategy
import net.cydhra.acromantula.workspace.WorkspaceService
import net.cydhra.acromantula.workspace.filesystem.FileEntity
import java.io.ByteArrayInputStream
import java.io.PushbackInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Import .art banks of CT3. They contain a set of GXT(X) files, which are GT Texture files.
 */
class ArtArchiveImporterStrategy : ImporterStrategy {
    override fun handles(fileName: String, fileContent: PushbackInputStream): Boolean {
        // .art does not seem to have magic bytes. How dare they?
        return fileName.endsWith(".art")
    }

    @Suppress("UsePropertyAccessSyntax")
    override fun import(parent: FileEntity?, fileName: String, fileContent: PushbackInputStream) {
        val byteArray = fileContent.readBytes()
        val buffer = ByteBuffer
            .wrap(byteArray)
            .order(ByteOrder.LITTLE_ENDIAN)

        // add archive entry for the bank
        val archiveEntity = WorkspaceService.addArchiveEntry(fileName, parent)

        val imageNumber = buffer.getInt()
        val offsetTable = IntArray(imageNumber + 1) { if (it < imageNumber) buffer.getInt() else byteArray.size }

        for (index in (0 until imageNumber)) {
            ImporterFeature.importFile(
                archiveEntity, "image$index",
                ByteArrayInputStream(
                    byteArray,
                    offsetTable[index],
                    offsetTable[index + 1] - offsetTable[index]
                )
            )
        }
    }

}