package net.cydhra.acromantula.gx.util

import java.awt.image.BufferedImage
import java.awt.image.Raster
import kotlin.experimental.and

/*
* Copyright 2009, Morten Nobel-Joergensen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/ /**
 * @author Heinz Doerr
 */
object ImageUtils {
    /**
     * @param img
     * @return
     */
    fun imageTypeName(img: BufferedImage): String {
        when (img.type) {
            BufferedImage.TYPE_3BYTE_BGR -> return "TYPE_3BYTE_BGR"
            BufferedImage.TYPE_4BYTE_ABGR -> return "TYPE_4BYTE_ABGR"
            BufferedImage.TYPE_4BYTE_ABGR_PRE -> return "TYPE_4BYTE_ABGR_PRE"
            BufferedImage.TYPE_BYTE_BINARY -> return "TYPE_BYTE_BINARY"
            BufferedImage.TYPE_BYTE_GRAY -> return "TYPE_BYTE_GRAY"
            BufferedImage.TYPE_BYTE_INDEXED -> return "TYPE_BYTE_INDEXED"
            BufferedImage.TYPE_CUSTOM -> return "TYPE_CUSTOM"
            BufferedImage.TYPE_INT_ARGB -> return "TYPE_INT_ARGB"
            BufferedImage.TYPE_INT_ARGB_PRE -> return "TYPE_INT_ARGB_PRE"
            BufferedImage.TYPE_INT_BGR -> return "TYPE_INT_BGR"
            BufferedImage.TYPE_INT_RGB -> return "TYPE_INT_RGB"
            BufferedImage.TYPE_USHORT_555_RGB -> return "TYPE_USHORT_555_RGB"
            BufferedImage.TYPE_USHORT_565_RGB -> return "TYPE_USHORT_565_RGB"
            BufferedImage.TYPE_USHORT_GRAY -> return "TYPE_USHORT_GRAY"
        }
        return "unknown image type #" + img.type
    }

    /**
     * @param img
     * @return
     */
    fun nrChannels(img: BufferedImage): Int {
        when (img.type) {
            BufferedImage.TYPE_3BYTE_BGR -> return 3
            BufferedImage.TYPE_4BYTE_ABGR -> return 4
            BufferedImage.TYPE_BYTE_GRAY -> return 1
            BufferedImage.TYPE_INT_BGR -> return 3
            BufferedImage.TYPE_INT_ARGB -> return 4
            BufferedImage.TYPE_INT_RGB -> return 3
            BufferedImage.TYPE_CUSTOM -> return 4
            BufferedImage.TYPE_4BYTE_ABGR_PRE -> return 4
            BufferedImage.TYPE_INT_ARGB_PRE -> return 4
            BufferedImage.TYPE_USHORT_555_RGB -> return 3
            BufferedImage.TYPE_USHORT_565_RGB -> return 3
            BufferedImage.TYPE_USHORT_GRAY -> return 1
        }
        return 0
    }

    /**
     *
     * returns one row (height == 1) of byte packed image data in BGR or AGBR form
     *
     * @param img
     * @param y
     * @param w
     * @param array
     * @param temp must be either null or a array with length of w*h
     * @return
     */
    fun getPixelsBGR(
        img: BufferedImage,
        y: Int, w: Int, array: ByteArray, temp: IntArray
    ): ByteArray {
        val x = 0
        val h = 1
        assert(array.size == temp.size * nrChannels(img))
        assert(temp.size == w)
        val imageType = img.type
        val raster: Raster
        when (imageType) {
            BufferedImage.TYPE_3BYTE_BGR, BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_4BYTE_ABGR_PRE, BufferedImage.TYPE_BYTE_GRAY -> {
                raster = img.raster
                //int ttype= raster.getTransferType();
                raster.getDataElements(x, y, w, h, array)
            }
            BufferedImage.TYPE_INT_BGR -> {
                raster = img.raster
                raster.getDataElements(x, y, w, h, temp)
                ints2bytes(temp, array, 0, 1, 2) // bgr -->  bgr
            }
            BufferedImage.TYPE_INT_RGB -> {
                raster = img.raster
                raster.getDataElements(x, y, w, h, temp)
                ints2bytes(temp, array, 2, 1, 0) // rgb -->  bgr
            }
            BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE -> {
                raster = img.raster
                raster.getDataElements(x, y, w, h, temp)
                ints2bytes(temp, array, 2, 1, 0, 3) // argb -->  abgr
            }
            BufferedImage.TYPE_CUSTOM -> {
                img.getRGB(x, y, w, h, temp, 0, w)
                ints2bytes(temp, array, 2, 1, 0, 3) // argb -->  abgr
            }
            else -> {
                img.getRGB(x, y, w, h, temp, 0, w)
                ints2bytes(temp, array, 2, 1, 0) // rgb -->  bgr
            }
        }
        return array
    }

    /**
     * converts and copies byte packed  BGR or ABGR into the img buffer,
     * the img type may vary (e.g. RGB or BGR, int or byte packed)
     * but the number of components (w/o alpha, w alpha, gray) must match
     *
     * does not unmange the image for all (A)RGN and (A)BGR and gray imaged
     * @param bgrPixels
     * @param img
     * @param x
     * @param y
     * @param w
     * @param h
     */
    fun setBGRPixels(
        bgrPixels: ByteArray,
        img: BufferedImage, x: Int, y: Int, w: Int, h: Int
    ) {
        val imageType = img.type
        val raster = img.raster
        //int ttype= raster.getTransferType();
        if (imageType == BufferedImage.TYPE_3BYTE_BGR || imageType == BufferedImage.TYPE_4BYTE_ABGR || imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE || imageType == BufferedImage.TYPE_BYTE_GRAY) {
            raster.setDataElements(x, y, w, h, bgrPixels)
        } else {
            val pixels: IntArray
            pixels = if (imageType == BufferedImage.TYPE_INT_BGR) {
                bytes2int(bgrPixels, 2, 1, 0) // bgr -->  bgr
            } else if (imageType == BufferedImage.TYPE_INT_ARGB ||
                imageType == BufferedImage.TYPE_INT_ARGB_PRE
            ) {
                bytes2int(bgrPixels, 3, 0, 1, 2) // abgr -->  argb
            } else {
                bytes2int(bgrPixels, 0, 1, 2) // bgr -->  rgb
            }
            if (w == 0 || h == 0) {
                return
            } else require(pixels.size >= w * h) { "pixels array must have a length" + " >= w*h" }
            if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB || imageType == BufferedImage.TYPE_INT_ARGB_PRE || imageType == BufferedImage.TYPE_INT_BGR) {
                raster.setDataElements(x, y, w, h, pixels)
            } else {
                // Unmanages the image
                img.setRGB(x, y, w, h, pixels, 0, w)
            }
        }
    }

    /**
     * @param in
     * @param out
     * @param index1
     * @param index2
     * @param index3
     */
    fun ints2bytes(
        `in`: IntArray,
        out: ByteArray, index1: Int,
        index2: Int, index3: Int
    ) {
        for (i in `in`.indices) {
            val index = i * 3
            var value = `in`[i]
            out[index + index1] = value.toByte()
            value = value shr 8
            out[index + index2] = value.toByte()
            value = value shr 8
            out[index + index3] = value.toByte()
        }
    }

    /**
     * @param in
     * @param out
     * @param index1
     * @param index2
     * @param index3
     * @param index4
     */
    fun ints2bytes(
        `in`: IntArray, out: ByteArray,
        index1: Int, index2: Int,
        index3: Int, index4: Int
    ) {
        for (i in `in`.indices) {
            val index = i * 4
            var value = `in`[i]
            out[index + index1] = value.toByte()
            value = value shr 8
            out[index + index2] = value.toByte()
            value = value shr 8
            out[index + index3] = value.toByte()
            value = value shr 8
            out[index + index4] = value.toByte()
        }
    }

    /**
     * @param in
     * @param index1
     * @param index2
     * @param index3
     * @return
     */
    fun bytes2int(
        `in`: ByteArray, index1: Int,
        index2: Int, index3: Int
    ): IntArray {
        val out = IntArray(`in`.size / 3)
        for (i in out.indices) {
            val index = i * 3
            val b1: Int = (`in`[index + index1] and (0xff shl 16).toByte()).toInt()
            val b2: Int = (`in`[index + index2] and (0xff shl 8).toByte()).toInt()
            val b3: Int = (`in`[index + index3] and 0xff.toByte()).toInt()
            out[i] = b1 or b2 or b3
        }
        return out
    }

    /**
     * @param in
     * @param index1
     * @param index2
     * @param index3
     * @param index4
     * @return
     */
    fun bytes2int(
        `in`: ByteArray, index1: Int,
        index2: Int, index3: Int, index4: Int
    ): IntArray {
        val out = IntArray(`in`.size / 4)
        for (i in out.indices) {
            val index = i * 4
            val b1: Int = (`in`[index + index1] and (0xff shl 24).toByte()).toInt()
            val b2: Int = (`in`[index + index2] and ((0xff shl 16).toByte())).toInt()
            val b3: Int = (`in`[index + index3] and (0xff shl 8).toByte()).toInt()
            val b4: Int = (`in`[index + index4] and 0xff.toByte()).toInt()
            out[i] = b1 or b2 or b3 or b4
        }
        return out
    }

    /**
     * Converts the [BufferedImage] type.
     * @param srcImage
     * @param destImgType
     * @return
     */
    fun convert(srcImage: BufferedImage, destImgType: Int): BufferedImage {
        val img = BufferedImage(srcImage.width, srcImage.height, destImgType)
        val g2d = img.createGraphics()
        g2d.drawImage(srcImage, 0, 0, null)
        g2d.dispose()
        return img
    }
}