package com.kylecorry.trail_sense.shared.andromeda_temp

/**
 * Unsigned little endian integers stored by byte: byte 0 of every value, then byte 1 of every value, and so on.
 */
object BytePlanes {

    /**
     * Reads [count] values of [bytes] bytes each (1 to 7), starting at [offset].
     */
    fun readUnsigned(data: ByteArray, offset: Int, count: Int, bytes: Int): LongArray {
        require(bytes in 1..7) { "Values must be 1 to 7 bytes" }
        require(offset >= 0 && offset.toLong() + count.toLong() * bytes <= data.size) { "The planes are past the end" }
        val values = LongArray(count)
        for (b in 0 until bytes) {
            val planeStart = offset + b * count
            for (i in 0 until count) {
                values[i] = values[i] or ((data[planeStart + i].toLong() and 0xFF) shl (8 * b))
            }
        }
        return values
    }
}
