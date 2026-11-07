package com.kylecorry.trail_sense.shared.andromeda_temp

/**
 * Reads unsigned LEB128 varints (7 bits per byte, least significant first, with the high bit set on every byte but the
 * last) one after another.
 */
class VarintReader(private val data: ByteArray, offset: Int = 0) {

    var offset = offset
        private set

    fun nextLong(): Long {
        var value = 0L
        var shift = 0
        while (true) {
            require(shift < Long.SIZE_BITS) { "The varint is too long" }
            val byte = data[offset++].toInt() and 0xFF
            value = value or ((byte and 0x7F).toLong() shl shift)
            if (byte < 0x80) {
                return value
            }
            shift += 7
        }
    }

    fun nextInt(): Int {
        val value = nextLong()
        require(value in 0..Int.MAX_VALUE) { "The varint does not fit in an int" }
        return value.toInt()
    }
}
