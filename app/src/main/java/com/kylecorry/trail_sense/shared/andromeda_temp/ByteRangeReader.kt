package com.kylecorry.trail_sense.shared.andromeda_temp

import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Reads pieces of a file without loading the rest of it.
 */
fun interface ByteRangeReader : Closeable {

    suspend fun read(offset: Long, length: Int): ByteArray

    override fun close() {
        // Nothing to close by default
    }

    companion object {
        fun fromBytes(bytes: ByteArray): ByteRangeReader {
            return ByteRangeReader { offset, length ->
                if (offset < 0 || length < 0 || offset + length > bytes.size) {
                    throw EOFException()
                }
                bytes.copyOfRange(offset.toInt(), (offset + length).toInt())
            }
        }

        fun fromFile(file: File): ByteRangeReader {
            return ChannelByteRangeReader {
                val randomAccessFile = RandomAccessFile(file, "r")
                ChannelByteRangeReader.OpenChannel(randomAccessFile.channel, 0, randomAccessFile.length(), randomAccessFile)
            }
        }

        /**
         * Reads [length] bytes at [offset] of a file that is [size] bytes long and starts at [start] in the channel.
         */
        fun readFully(channel: FileChannel, start: Long, size: Long, offset: Long, length: Int): ByteArray {
            if (offset < 0 || length < 0 || offset + length > size) {
                throw EOFException()
            }
            val buffer = ByteBuffer.allocate(length)
            while (buffer.hasRemaining()) {
                if (channel.read(buffer, start + offset + buffer.position()) < 0) {
                    throw EOFException()
                }
            }
            return buffer.array()
        }
    }
}
