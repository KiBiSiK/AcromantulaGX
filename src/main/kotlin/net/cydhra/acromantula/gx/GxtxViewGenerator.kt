package net.cydhra.acromantula.gx

import com.github.memo33.jsquish.Squish
import net.cydhra.acromantula.features.view.ViewGeneratorStrategy
import net.cydhra.acromantula.gx.util.ByteBufferedImage
import net.cydhra.acromantula.workspace.WorkspaceService
import net.cydhra.acromantula.workspace.disassembly.FileRepresentation
import net.cydhra.acromantula.workspace.filesystem.FileEntity
import net.cydhra.acromantula.workspace.filesystem.FileType
import net.cydhra.acromantula.workspace.filesystem.pngFileType
import org.apache.logging.log4j.LogManager
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import kotlin.experimental.and
import kotlin.math.pow

/**
 * Generate a PNG from a GT-Texture so the user can view and/or export it.
 */
class GxtxViewGenerator : ViewGeneratorStrategy {
    companion object {
        private val logger = LogManager.getLogger()
    }

    override val viewType: String = "gxtx"

    override val fileType: FileType = pngFileType

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
        contentBuffer.getInt()
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

        val img = convertDataToImage(dataBuffer, width, height, compressionType)
        val byteArrayOutputStream = ByteArrayOutputStream()

        // images are stored up-side down, therefore flip them before storing
        val flipTransform = AffineTransformOp(
            AffineTransform.getScaleInstance(1.0, -1.0).also { it.translate(0.0, (-height).toDouble()) },
            AffineTransformOp.TYPE_NEAREST_NEIGHBOR
        )

        // write image as png into file system
        ImageIO.write(flipTransform.filter(img, null), "png", byteArrayOutputStream)
        return WorkspaceService.addFileRepresentation(fileEntity, this.viewType, byteArrayOutputStream.toByteArray())
    }

    /**
     * Convert a byte array into a standard [BufferedImage] depending on the given [encoding]. No additional operations
     * besides decoding and/or decompression are performed upon the data.
     *
     * @param data the byte array read from the gxtx resource
     * @param width image width read in header
     * @param height image height read in header
     * @param encoding [GxtxCompressionType] read in header
     */
    private fun convertDataToImage(
        data: ByteArray,
        width: Int,
        height: Int,
        encoding: GxtxCompressionType
    ): BufferedImage {
        return when (encoding) {
            GxtxCompressionType.DXT1 -> {
                ByteBufferedImage(
                    width,
                    height,
                    Squish.decompressImage(null, width, height, data, Squish.CompressionType.DXT1)
                )
            }
            GxtxCompressionType.DXT3 -> {
                ByteBufferedImage(
                    width,
                    height,
                    Squish.decompressImage(null, width, height, data, Squish.CompressionType.DXT3)
                )
            }
            GxtxCompressionType.A4R4G4B4 -> {
                val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val imageBuffer = img.raster.dataBuffer
                var i = 0
                // TODO: warning, this code does not produce sane results. The actual encoding in the resources is not simple ARGB4
                while (i < data.size) {
                    val alpha = ((data[i] and 0xF0.toByte()).toInt() shr 4) * 2.0.pow(4.0).toInt()
                    val red = (data[i] and 0x0F.toByte()).toInt() * 2.0.pow(4.0).toInt()
                    val green = ((data[i + 1] and 0xF0.toByte()).toInt() shr 4) * 2.0.pow(4.0).toInt()
                    val blue = (data[i + 1] and 0x0F.toByte()).toInt() * 2.0.pow(4.0).toInt()
                    val argb = (alpha shl 24) or (red shl 16) or (green shl 8) or (blue)
                    imageBuffer.setElem(i / 2, argb)
                    i += 2
                }

                img
            }
            GxtxCompressionType.A8R8G8B8 -> {
                val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val imageBuffer = img.raster.dataBuffer
                var i = 0
                // TODO: warning, this code does not produce sane results. The actual encoding in the resources is not simple A8R8G8B8
                while (i < data.size) {
                    val alpha = data[i].toInt()
                    val red = data[i + 1].toInt()
                    val green = data[i + 2].toInt()
                    val blue = data[i + 3].toInt()
                    val argb = (alpha shl 24) or (red shl 16) or (green shl 8) or (blue)
                    imageBuffer.setElem(i / 4, argb)
                    i += 4
                }

                img
            }
            GxtxCompressionType.A1R5G5B5 -> {
                val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val imageBuffer = img.raster.dataBuffer
                var i = 0
                // TODO: warning, this code does not produce sane results. The actual encoding in the resources is not simple A1R5G5B5
                while (i < data.size) {
                    val alpha = (data[i].toInt() shr 7) // shr is sign-extending
                    val red = (data[i].toInt() and 0b0111_1100) shl 1
                    val green = (((data[i].toInt() shl 8) or (data[i + 1].toInt())) and 0b0000_0011_1110_0000) shr 2
                    val blue = data[i + 1].toInt() shl 3
                    val argb = (alpha shl 24) or (red shl 16) or (green shl 8) or (blue)
                    imageBuffer.setElem(i / 2, argb)
                    i += 2
                }
                img
            }
            GxtxCompressionType.R5G6B5 -> {
                val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val imageBuffer = img.raster.dataBuffer
                var i = 0
                // TODO: warning, this code does not produce sane results. The actual encoding in the resources is not simple R5G6B5
                while (i < data.size) {
                    val red = (data[i].toInt() and 0b1111_1000)
                    val green = (((data[i].toInt() shl 8) or (data[i + 1].toInt())) and 0b0000_0111_1110_0000) shr 3
                    val blue = data[i + 1].toInt() shl 3
                    val argb = (0xFF shl 24) or (red shl 16) or (green shl 8) or (blue)
                    imageBuffer.setElem(i / 2, argb)
                    i += 2
                }
                img
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