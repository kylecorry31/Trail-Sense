package com.kylecorry.trail_sense.shared.data

import java.io.EOFException

fun interface ByteRangeReader {

    suspend fun read(offset: Long, length: Int): ByteArray

    companion object {
        fun fromBytes(bytes: ByteArray): ByteRangeReader {
            return ByteRangeReader { offset, length ->
                if (offset < 0 || offset + length > bytes.size) {
                    throw EOFException()
                }
                bytes.copyOfRange(offset.toInt(), (offset + length).toInt())
            }
        }
    }
}
