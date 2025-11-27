package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.core.units.PixelCoordinate

class PixelResult<T>(
    val x: Int,
    val y: Int,
    val value: T
) {
    val coordinate: PixelCoordinate
        get() = PixelCoordinate(x.toFloat(), y.toFloat())
}