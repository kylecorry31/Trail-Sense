package com.kylecorry.trail_sense.shared.canvas

import android.graphics.Path
import com.kylecorry.andromeda.canvas.Paths
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlin.math.cos
import kotlin.math.sin

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
        return dialTicks(center.x, center.y, radius, tickLength, spacing, start, end, path)
    }

    private fun dialTicks(
        x: Float,
        y: Float,
        radius: Float,
        tickLength: Float,
        spacing: Int,
        start: Int = 0,
        end: Int = 360,
        path: Path = Path()
    ): Path {
        return path.apply {
            reset()
            for (angle in start..end step spacing) {
                if (angle == end && start == end) {
                    continue
                }
                val tickX = cos(Math.toRadians(angle.toDouble()).toFloat())
                val tickY = sin(Math.toRadians(angle.toDouble()).toFloat())
                moveTo(x + tickX * (radius - tickLength / 2), y + tickY * (radius - tickLength / 2))
                lineTo(x + tickX * (radius + tickLength / 2), y + tickY * (radius + tickLength / 2))
            }
        }
    }

}
