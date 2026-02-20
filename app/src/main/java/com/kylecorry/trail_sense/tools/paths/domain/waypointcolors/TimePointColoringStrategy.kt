package com.kylecorry.trail_sense.tools.paths.domain.waypointcolors

import android.util.Range
import com.kylecorry.sol.math.arithmetic.Arithmetic.clamp
import com.kylecorry.sol.math.interpolation.Interpolation.norm
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import java.time.Instant

class TimePointColoringStrategy(
    private val timeRange: Range<Instant>,
    private val colorScale: IColorScale
) : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int? {
        val time = point.time ?: return null
        val ratio = clamp(
            norm(
                time.toEpochMilli().toFloat(),
                timeRange.lower.toEpochMilli().toFloat(),
                timeRange.upper.toEpochMilli().toFloat()
            ), 0f, 1f
        )
        return colorScale.getColor(ratio)
    }
}