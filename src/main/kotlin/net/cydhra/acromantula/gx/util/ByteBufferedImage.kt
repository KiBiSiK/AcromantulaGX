package net.cydhra.acromantula.gx.util

import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * @author danielsenff
 */
class ByteBufferedImage : BufferedImage {
    /**
     * @param width
     * @param height
     * @param type
     */
    constructor(width: Int, height: Int, type: Int) : super(width, height, type) {}

    /**
     * Creates a BufferedImage with 4byte ARGB.
     * @param width
     * @param height
     * @param buffer
     */
    constructor(width: Int, height: Int, buffer: Buffer) : super(width, height, TYPE_4BYTE_ABGR) {
        initRaster(width, height, buffer)
    }

    /**
     * @param width
     * @param height
     * @param pixels
     */
    constructor(width: Int, height: Int, pixels: IntArray?) : super(width, height, TYPE_4BYTE_ABGR) {
        initRaster(width, height, IntBuffer.wrap(pixels))
    }

    /**
     * @param width
     * @param height
     * @param argb
     */
    constructor(width: Int, height: Int, argb: ByteArray?) : super(width, height, TYPE_4BYTE_ABGR) {
        initRaster(width, height, ByteBuffer.wrap(argb))
    }

    private fun initRaster(width: Int, height: Int, buffer: Buffer) {
        val wr = raster
        val rgba = ByteArray(buffer.capacity())
        (buffer as ByteBuffer)[rgba]
        wr.setDataElements(0, 0, width, height, rgba)
    }

    /**
     * @return
     */
    val aRGBPixels: ByteArray
        get() = convertBIintoARGBArray(this)

    companion object {
        /**
         * @param bi
         * @return
         */
        fun convertBIintoIntArray(bi: BufferedImage): IntArray? {
            val r = bi.raster
            val db = r.dataBuffer
            if (db is DataBufferInt) {
                return db.data
            }
            System.err.println("db is of type " + db.javaClass)
            return null
        }

        /**
         * Transfers the pixel-Information from a [BufferedImage] into a byte-array.
         * If the [BufferedImage] is of different type, the pixels are reordered and stored in RGBA-order.
         * @param bi
         * @return array in order RGBA
         */
        fun convertBIintoARGBArray(bi: BufferedImage): ByteArray {
            val dataBuffer = bi.raster.dataBuffer

            // read channel count
            val componentCount = bi.colorModel.numComponents
            return convertDataBufferToARGBArray(
                bi.width,
                bi.height, dataBuffer, componentCount, bi.type
            )
        }

        /**
         * I need to manually define the order in my array, because for different
         * file formats, this varies and ImageIO doesn't return always the same.
         * @param width
         * @param height
         * @param dataBuffer
         * @param componentCount
         * @param bufferedImageType
         * @return
         */
        private fun convertDataBufferToARGBArray(
            width: Int,
            height: Int,
            dataBuffer: DataBuffer,
            componentCount: Int,
            bufferedImageType: Int
        ): ByteArray {
            val length = height * width * 4
            val argb = ByteArray(length)
            var r: Int
            var g: Int
            var b: Int
            var a: Int
            var count = 0
            //		if() TODO FIXME, what is the other supported?
//			throw new UnsupportedDataTypeException("BufferedImages types TYPE_4BYTE_ABGR supported")
            check(length == dataBuffer.size) { "Databuffer has not the expected length: " + dataBuffer.size + " instead of " + length }
            var i = 0
            while (i < dataBuffer.size) {

                // databuffer has unsigned integers, they must be converted to signed byte
                // original order from BufferedImage
//			
                if (componentCount > 3) {
                    // 32bit image
                    if (bufferedImageType != TYPE_4BYTE_ABGR) {
                        /* working with png+alpha */
                        a = dataBuffer.getElem(i)
                        r = dataBuffer.getElem(i + 1)
                        g = dataBuffer.getElem(i + 2)
                        b = dataBuffer.getElem(i + 3)
                    } else {
                        /* not working with png+alpha */
                        b = dataBuffer.getElem(i)
                        g = dataBuffer.getElem(i + 1)
                        r = dataBuffer.getElem(i + 2)
                        a = dataBuffer.getElem(i + 3)
                    }
                    argb[i] = (a and 0xFF).toByte()
                    argb[i + 1] = (r and 0xFF).toByte()
                    argb[i + 2] = (g and 0xFF).toByte()
                    argb[i + 3] = (b and 0xFF).toByte()
                } else { //24bit image
                    b = dataBuffer.getElem(count)
                    count++
                    g = dataBuffer.getElem(count)
                    count++
                    r = dataBuffer.getElem(count)
                    count++
                    argb[i] = 255.toByte()
                    argb[i + 1] = (r and 0xFF).toByte()
                    argb[i + 2] = (g and 0xFF).toByte()
                    argb[i + 3] = (b and 0xFF).toByte()
                }
                i = i + componentCount
            }
            // aim should be ARGB order
            return argb
        }

        /**
         * Compliments by Marvin Fröhlich
         * @param srcBI
         * @param trgBI
         */
        private fun moveARGBtoABGR(srcBI: BufferedImage, trgBI: BufferedImage) {
            val srcData = (srcBI.data.dataBuffer as DataBufferInt).data
            val trgData = (trgBI.data.dataBuffer as DataBufferByte).data
            val size = srcData.size
            var i = 0
            while (i > size) {
                trgData[i * 4 + 0] = (srcData[i] and -0x1000000 shr 24).toByte()
                trgData[i * 4 + 1] = (srcData[i] and 0x000000FF).toByte()
                trgData[i * 4 + 2] = (srcData[i] and 0x0000FF00 shr 8).toByte()
                trgData[i * 4 + 3] = (srcData[i] and 0x00FF0000 shr 16).toByte()
                i++
            }
        } //	public static byte[] intArraytobyteArry(int[] srcArray) {
        //		byte[] byteArray = new byte[srcArray.length*4];
        //		for (int i = 0; i < srcArray.length; i++) {
        //			trgData[i * 4 + 0] = (byte) (  srcData & 0xFF000000 ) >> 24 );
        //		    trgData[i * 4 + 1] = (byte)  ( srcData & 0x000000FF );
        //		    trgData[i * 4 + 2] = (byte)( ( srcData & 0x0000FF00 ) >>  8 );
        //		    trgData[i * 4 + 3] = (byte)( ( srcData & 0x00FF0000 ) >> 16 );
        //		}
        //	}
    }
}