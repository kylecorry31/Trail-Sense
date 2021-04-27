package com.kylecorry.trail_sense.shared

@ExperimentalUnsignedTypes
fun ByteArray.toShort(): Short {
    if (size != Short.SIZE_BYTES){
        throw IllegalStateException("Byte array does not contain a short")
    }
    return ((get(0).toUByte().toInt() shl 8) + get(1).toUByte().toInt()).toShort()
}

@ExperimentalUnsignedTypes
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
fun ByteArray.toInt(): Int {
    if (size != Int.SIZE_BYTES){
        throw IllegalStateException("Byte array does not contain a short")
    }

    var intValue = 0u
    for (i in 0 until Int.SIZE_BYTES){
        intValue = intValue or get(i).toUByte().toUInt() shl 8 * (Int.SIZE_BYTES - i)
    }

    return intValue.toInt()
}