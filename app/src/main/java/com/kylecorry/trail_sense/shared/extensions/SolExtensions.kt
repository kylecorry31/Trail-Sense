package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.math.geometry.Size
import kotlin.math.absoluteValue

fun Size.toAndroidSize(): android.util.Size {
    return android.util.Size(width.toInt(), height.toInt())
}

fun Float.real(defaultValue: Float = 0f): Float {
    return if (this.isNaN() || this.isInfinite()) defaultValue else this
}

fun Float.positive(zeroReplacement: Float = 1f): Float {
    return if (this < 0) {
        absoluteValue
    } else if (this == 0f) {
        zeroReplacement
    } else {
        this
    }
}