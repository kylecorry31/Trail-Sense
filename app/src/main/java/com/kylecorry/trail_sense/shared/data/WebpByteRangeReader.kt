package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Size
import com.kylecorry.luna.concurrency.onIO
import java.io.EOFException
import java.io.IOException

class WebpByteRangeReader(private val image: ImageReader) : ByteRangeReader {

    override suspend fun read(offset: Long, length: Int): ByteArray {
        val size = image.getSize()
        val totalBytes = size.width.toLong() * size.height
        val outOfRange = offset < 0 || length < 0 || offset + length > totalBytes
        if (size.width != WIDTH || outOfRange) {
            throw EOFException()
        }

        // Rows aligned to the image reader's blocks, so it decodes them as they are (it cannot crop non-premultiplied bitmaps)
        val firstRow = (offset / WIDTH).toInt() / BLOCK_ROWS * BLOCK_ROWS
        val lastRow = ((offset + length - 1) / WIDTH).toInt()
        val endRow = minOf((lastRow / BLOCK_ROWS + 1) * BLOCK_ROWS, size.height)

        val bitmap = image.getRegion(Rect(0, firstRow, WIDTH, endRow), Bitmap.Config.ARGB_8888)
            ?: throw IOException("Could not read the image")

        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val bytes = ByteArray(length)
            val start = (offset - firstRow.toLong() * WIDTH).toInt()
            for (i in 0 until length) {
                bytes[i] = pixels[start + i].toByte()
            }
            return bytes
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        const val WIDTH = 64
        private const val BLOCK_ROWS = 16

        suspend fun open(image: InputStreamable): WebpByteRangeReader? = onIO {
            val stream = image.getInputStream() ?: return@onIO null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            stream.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@onIO null
            }
            WebpByteRangeReader(
                SingleImageReader(Size(options.outWidth, options.outHeight), image, premultiplied = false)
            )
        }
    }
}
