package com.kylecorry.trail_sense.shared.canvas

import android.graphics.Path
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath

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
        return path.apply {
            reset()
            for (angle in start..end step spacing) {
                if (angle == end && start == end) {
                    continue
                }
                val x = SolMath.cosDegrees(angle.toFloat())
                val y = SolMath.sinDegrees(angle.toFloat())
                moveTo(center.x + x * (radius - tickLength), center.y + y * (radius - tickLength))
                lineTo(center.x + x * radius, center.y + y * radius)
            }
        }
    }

}