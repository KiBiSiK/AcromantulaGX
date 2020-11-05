package net.cydhra.acromantula.gx

import com.github.memo33.jsquish.Squish
import net.cydhra.acromantula.features.view.ViewGeneratorStrategy
import net.cydhra.acromantula.gx.util.ByteBufferedImage
import net.cydhra.acromantula.workspace.WorkspaceService
import net.cydhra.acromantula.workspace.disassembly.FileRepresentation
import net.cydhra.acromantula.workspace.filesystem.FileEntity
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/**
 * Generate a PNG from a GT-Texture so the user can view and/or export it.
 */
class GxtxViewGenerator : ViewGeneratorStrategy {
    companion object {
        private val logger = LogManager.getLogger()
    }

    override val viewType: String = "gxtx"

    override fun handles(fileEntity: FileEntity): Boolean {
        val content = WorkspaceService.getFileContent(fileEntity)
        val buffer = ByteArray(4)
        val readSize = content.read(buffer, 0, 4)

        // "GXTX" magic bytes
        return readSize == 4 && byteArrayOf(
            0x47.toByte(),
            0x58.toByte(),
            0x54.toByte(),
            0x58.toByte(),
        ).contentEquals(buffer)
    }

    @Suppress("UsePropertyAccessSyntax")
    override fun generateView(fileEntity: FileEntity): FileRepresentation {
        val contentBuffer = ByteBuffer
            .wrap(WorkspaceService.getFileContent(fileEntity).readBytes())
            .order(ByteOrder.LITTLE_ENDIAN)

        // skip magic bytes
        contentBuffer.position(4)
        val width = contentBuffer.getShort().toInt()
        val height = contentBuffer.getShort().toInt()
        val dataBufferSize = contentBuffer.getInt()

        // skip unknown magic
        contentBuffer.getShort()

        val compressionTypeDescriptor = contentBuffer.getShort()
        val compressionType = GxtxCompressionType.byDescriptor(compressionTypeDescriptor.toInt())

        val dataBuffer = ByteArray(dataBufferSize)
        contentBuffer.get(dataBuffer)

        logger.debug(
            "decompressing GXTX resource [$width x $height; $dataBufferSize bytes; " +
                    "compression: $compressionType]"
        )

        return when (compressionType) {
            GxtxCompressionType.DXT1 -> {
                val decompressedData =
                    Squish.decompressImage(null, width, height, dataBuffer, Squish.CompressionType.DXT1)
                val img = ByteBufferedImage(width, height, decompressedData)
                val byteArrayOutputStream = ByteArrayOutputStream()
                ImageIO.write(img, "png", byteArrayOutputStream)

                WorkspaceService.addFileRepresentation(fileEntity, this.viewType, byteArrayOutputStream.toByteArray())
            }
            GxtxCompressionType.DXT3 -> {
                val decompressedData =
                    Squish.decompressImage(null, width, height, dataBuffer, Squish.CompressionType.DXT3)
                val img = ByteBufferedImage(width, height, decompressedData)
                val byteArrayOutputStream = ByteArrayOutputStream()
                ImageIO.write(img, "png", byteArrayOutputStream)

                WorkspaceService.addFileRepresentation(fileEntity, this.viewType, byteArrayOutputStream.toByteArray())
            }
            // TODO decompress remaining compression formats
            else -> {
                logger.warn("decompression failed, decompression algorithm unsupported.")
                WorkspaceService.addFileRepresentation(fileEntity, this.viewType, dataBuffer)
            }
        }
    }
}

/**
 * Compression types used in GXTX format
 */
private enum class GxtxCompressionType {
    DXT1, DXT3, A8R8G8B8, A4R4G4B4, R5G6B5, A1R5G5B5;

    companion object {
        fun byDescriptor(descriptor: Int): GxtxCompressionType {
            return when (descriptor) {
                10 -> DXT1
                11 -> DXT3
                4 -> A8R8G8B8
                5 -> A4R4G4B4
                0 -> R5G6B5
                -1 -> A1R5G5B5
                else -> throw IllegalArgumentException("unknown compression type: $descriptor")
            }
        }
    }
}