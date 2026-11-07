package com.kylecorry.trail_sense.shared.andromeda_temp

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.zip.DataFormatException
import java.util.zip.Deflater

class RawDeflateTest {

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(data)
        deflater.finish()
        val buffer = ByteArray(data.size + 64)
        val length = deflater.deflate(buffer)
        deflater.end()
        return buffer.copyOf(length)
    }

    private val original = ByteArray(10_000) { (it % 7).toByte() }

    @Test
    fun inflatesRawDeflate() {
        assertArrayEquals(original, RawDeflate.inflate(deflate(original), original.size))
    }

    @Test
    fun rejectsDataThatIsTooLarge() {
        assertThrows(DataFormatException::class.java) { RawDeflate.inflate(deflate(original), original.size - 1) }
    }

    @Test
    fun rejectsIncompleteData() {
        val compressed = deflate(original)
        assertThrows(DataFormatException::class.java) { RawDeflate.inflate(compressed.copyOf(compressed.size / 2), original.size) }
    }
}
