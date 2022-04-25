package com.kylecorry.trail_sense.shared.canvas

import android.graphics.Path
import com.kylecorry.andromeda.canvas.Paths
import com.kylecorry.andromeda.core.units.PixelCoordinate

object Dial {

    fun ticks(
        center: PixelCoordinate,
        radius: Float,
        tickLength: Float,
        spacing: Int,
        start: Int = 0,
        end: Int = 360,
        path: Path = Path()
    ): Path {
        return Paths.dialTicks(center.x, center.y, radius, tickLength, spacing, start, end, path)
    }

}