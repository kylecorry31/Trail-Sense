package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import java.io.InputStream

object CompressionUtils {

    @OptIn(ExperimentalUnsignedTypes::class)
    fun getShort(stream: InputStream, line: Int, closeStream: Boolean = true): Short? {
        val bytes =
            getBytes(stream, line, Short.SIZE_BYTES, closeStream = closeStream) ?: return null
        return bytes.toShort()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun getInt(stream: InputStream, line: Int, closeStream: Boolean = true): Int? {
        val bytes = getBytes(stream, line, Int.SIZE_BYTES, closeStream = closeStream) ?: return null
        return bytes.toInt()
    }

    fun getByte(
        stream: InputStream,
        line: Int,
        numBits: Int = Byte.SIZE_BITS,
        closeStream: Boolean = true
    ): Byte? {
        if (numBits > Byte.SIZE_BITS) throw IllegalArgumentException("Cannot read more than 8 bits")

        if (numBits == Byte.SIZE_BITS) {
            return getBytes(stream, line, 1, closeStream = closeStream)?.get(0)
        }

        val startBit = line * numBits
        val startByte = startBit / 8
        val startingOffset = startBit % 8

        val secondLineNeeded = (startBit + numBits) / 8 > startByte

        val bytes =
            getBytes(
                stream,
                startByte,
                1,
                lineCount = if (secondLineNeeded) 2 else 1,
                closeStream = closeStream
            ) ?: return null

        return bytes.bitsToByte(numBits, startingOffset)
    }

    fun getBytes(
        stream: InputStream,
        line: Int,
        bytesPerLine: Int,
        lineCount: Int = 1,
        closeStream: Boolean = true
    ): ByteArray? {
        var value: ByteArray?
        try {
            val desiredLine = bytesPerLine.toLong() * line
            stream.skip(desiredLine)
            value = ByteArray(bytesPerLine * lineCount)
            stream.read(value, 0, bytesPerLine * lineCount)
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