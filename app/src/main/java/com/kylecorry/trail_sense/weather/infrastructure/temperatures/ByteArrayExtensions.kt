package com.kylecorry.trail_sense.weather.infrastructure.temperatures

@ExperimentalUnsignedTypes
fun ByteArray.toShort(): Short {
    if (size != Short.SIZE_BYTES) {
        throw IllegalStateException("Byte array does not contain a short")
    }
    return ((get(0).toUByte().toInt() shl 8) or get(1).toUByte().toInt()).toShort()
}

@ExperimentalUnsignedTypes
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
fun ByteArray.toInt(): Int {
    if (size != Int.SIZE_BYTES) {
        throw IllegalStateException("Byte array does not contain a short")
    }

    var intValue = 0u
    for (i in 0 until Int.SIZE_BYTES) {
        intValue = intValue or get(i).toUByte().toUInt() shl 8 * (Int.SIZE_BYTES - i)
    }

    return intValue.toInt()
}

fun ByteArray.bitsToInt(numBits: Int, startingOffsetBits: Int = 0): Int? {
    if (numBits > Int.SIZE_BITS) throw IllegalArgumentException("Cannot read more than ${Int.SIZE_BITS} bits")
    var currentByteIndex = startingOffsetBits / 8
    var currentBitIndex = startingOffsetBits % 8

    var remainingBits = numBits
    var value = 0

    while (remainingBits > 0) {
        if (currentByteIndex >= this.size) {
            // Reached the end of the byte array before reading all bits
            return null
        }

        val currentByte = this[currentByteIndex].toInt()
        val bitsAvailable = 8 - currentBitIndex
        val bitsToRead = minOf(bitsAvailable, remainingBits)

        value =
            (value shl bitsToRead) or ((currentByte ushr (bitsAvailable - bitsToRead)) and ((1 shl bitsToRead) - 1))
        remainingBits -= bitsToRead
        currentBitIndex += bitsToRead

        if (currentBitIndex >= 8) {
            currentBitIndex = 0
            currentByteIndex++
        }
    }
    return value
}

fun ByteArray.bitsToByte(numBits: Int, startingOffsetBits: Int = 0): Byte? {
    if (numBits > Byte.SIZE_BITS) throw IllegalArgumentException("Cannot read more than 8 bits")
    return bitsToInt(numBits, startingOffsetBits)?.toByte()
}