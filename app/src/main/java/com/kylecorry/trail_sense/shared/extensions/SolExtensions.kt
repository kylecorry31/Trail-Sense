package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.math.geometry.Size

fun Size.toAndroidSize(): android.util.Size {
    return android.util.Size(width.toInt(), height.toInt())
}