package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GeoPointBlockDecoderTest {

    @Test
    fun rejectsAPointCountLargerThanTheBlockWhenThereAreNoFields() {
        val decoder = GeoPointBlockDecoder(GeoPointGrid(30, 4), emptyList())
        // One cell with a gap of 0 and a count of 2,000,000,000 (varint)
        val block = byteArrayOf(1, 0, 0x80.toByte(), 0xA8.toByte(), 0xD6.toByte(), 0xB9.toByte(), 0x07)

        assertThrows(IllegalArgumentException::class.java) { decoder.decode(block, 0, 10) }
    }
}
