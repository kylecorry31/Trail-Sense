package com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors

import android.util.Range
import com.kylecorry.andromeda.core.math.constrain
import com.kylecorry.andromeda.core.math.norm
import com.kylecorry.trail_sense.tools.backtrack.domain.scales.IColorScale
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class AltitudePointColoringStrategy(
    private val altitudeRange: Range<Float>,
    private val colorScale: IColorScale
) : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int {
        val altitude = point.elevation ?: return colorScale.getColor(0f)
        val ratio = constrain(norm(altitude, altitudeRange.lower, altitudeRange.upper), 0f, 1f)
        return colorScale.getColor(ratio)
    }
}