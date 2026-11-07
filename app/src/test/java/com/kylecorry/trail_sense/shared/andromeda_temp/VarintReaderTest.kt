package com.kylecorry.trail_sense.shared.andromeda_temp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class VarintReaderTest {

    @Test
    fun readsVarintsInOrder() {
        val data = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0x01, 0xE5.toByte(), 0x8E.toByte(), 0x26)
        val reader = VarintReader(data)

        assertEquals(0, reader.nextInt())
        assertEquals(127, reader.nextInt())
        assertEquals(128, reader.nextInt())
        assertEquals(624485L, reader.nextLong())
        assertEquals(data.size, reader.offset)
    }

    @Test
    fun startsAtTheOffset() {
        assertEquals(1, VarintReader(byteArrayOf(0x7F, 0x01), offset = 1).nextInt())
    }

    @Test
    fun rejectsValuesThatDoNotFit() {
        val tooLarge = byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x10)
        assertThrows(IllegalArgumentException::class.java) { VarintReader(tooLarge).nextInt() }
        assertThrows(IllegalArgumentException::class.java) { VarintReader(ByteArray(11) { 0x80.toByte() }).nextLong() }
        assertThrows(IndexOutOfBoundsException::class.java) { VarintReader(byteArrayOf(0x80.toByte())).nextLong() }
    }
}
