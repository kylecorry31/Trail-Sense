package com.kylecorry.trail_sense.tools.paths.domain.waypointcolors

import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.math.absoluteValue

class SlopePointColoringStrategy(private val colorScale: IColorScale) :
    IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int {
        val pct = if (point.slope.absoluteValue <= 10f) {
            0f
        } else if (point.slope.absoluteValue <= 25f) {
            0.5f
        } else {
            1f
        }
        return colorScale.getColor(pct)
    }
}