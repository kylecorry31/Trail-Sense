package com.kylecorry.trail_sense.shared.andromeda_temp

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Deflate data without a zlib or gzip header (RFC 1951).
 */
object RawDeflate {

    private const val BUFFER_BYTES = 8 * 1024

    /**
     * Decompresses [data], throwing a [DataFormatException] if it is incomplete, invalid, or more than [maxBytes] when
     * decompressed.
     */
    fun inflate(data: ByteArray, maxBytes: Int): ByteArray {
        val inflater = Inflater(true)
        try {
            inflater.setInput(data)
            val output = ByteArrayOutputStream(minOf(data.size.toLong() * 4, maxBytes.toLong()).toInt())
            val buffer = ByteArray(BUFFER_BYTES)
            while (!inflater.finished()) {
                val read = inflater.inflate(buffer)
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw DataFormatException("The data is incomplete")
                }
                output.write(buffer, 0, read)
                if (output.size() > maxBytes) {
                    throw DataFormatException("The data is larger than $maxBytes bytes")
                }
            }
            return output.toByteArray()
        } finally {
            inflater.end()
        }
    }
}
