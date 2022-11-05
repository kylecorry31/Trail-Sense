package com.kylecorry.trail_sense.shared.views.chart

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2

interface IChart {
    fun toPixel(data: Vector2): PixelCoordinate
    fun toData(pixel: PixelCoordinate): Vector2
    val xRange: Range<Float>
    val yRange: Range<Float>
}