package com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors

import android.util.Range
import com.kylecorry.andromeda.core.math.constrain
import com.kylecorry.andromeda.core.math.norm
import com.kylecorry.trail_sense.tools.backtrack.domain.scales.IColorScale
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import java.time.Instant

class TimePointColoringStrategy(
    private val timeRange: Range<Instant>,
    private val colorScale: IColorScale
) : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int {
        val time = point.time ?: return colorScale.getColor(0f)
        val ratio = constrain(
            norm(
                time.toEpochMilli().toFloat(),
                timeRange.lower.toEpochMilli().toFloat(),
                timeRange.upper.toEpochMilli().toFloat()
            ), 0f, 1f
        )
        return colorScale.getColor(ratio)
    }
}