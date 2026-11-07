package com.kylecorry.trail_sense.shared.andromeda_temp

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BytePlanesTest {

    @Test
    fun readsValuesStoredByByte() {
        // A header byte, then byte 0 of 0x0102, 0x03FF, 0xFF00, then byte 1 of each
        val data = byteArrayOf(0x55, 0x02, 0xFF.toByte(), 0x00, 0x01, 0x03, 0xFF.toByte())

        assertArrayEquals(longArrayOf(0x0102, 0x03FF, 0xFF00), BytePlanes.readUnsigned(data, 1, 3, 2))
        assertArrayEquals(longArrayOf(0x02, 0xFF), BytePlanes.readUnsigned(data, 1, 2, 1))
    }

    @Test
    fun rejectsPlanesPastTheEnd() {
        assertThrows(IllegalArgumentException::class.java) { BytePlanes.readUnsigned(ByteArray(5), 0, 3, 2) }
        assertThrows(IllegalArgumentException::class.java) { BytePlanes.readUnsigned(ByteArray(5), 0, 1, 8) }
    }
}
