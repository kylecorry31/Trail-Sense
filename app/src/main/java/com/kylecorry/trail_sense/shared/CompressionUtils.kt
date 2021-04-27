package com.kylecorry.trail_sense.shared

import java.io.InputStream

object CompressionUtils {

    fun getShort(stream: InputStream, line: Int, closeStream: Boolean = true): Short? {
        val bytes = getBytes(stream, line, Short.SIZE_BYTES, closeStream) ?: return null
        return bytes.toShort()
    }

    fun getInt(stream: InputStream, line: Int, closeStream: Boolean = true): Int? {
        val bytes = getBytes(stream, line, Int.SIZE_BYTES, closeStream) ?: return null
        return bytes.toInt()
    }

    fun getBytes(
        stream: InputStream,
        line: Int,
        bytes: Int,
        closeStream: Boolean = true
    ): ByteArray? {
        var value: ByteArray?
        try {
            val desiredLine = bytes.toLong() * line
            stream.skip(desiredLine)
            value = ByteArray(bytes)
            stream.read(value, 0, bytes)
        } catch (e: Exception) {
            value = null
        } finally {
            if (closeStream) {
                stream.close()
            }
        }
        return value
    }

}