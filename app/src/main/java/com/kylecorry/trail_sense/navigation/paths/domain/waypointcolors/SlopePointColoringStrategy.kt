package com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors

import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.scales.IColorScale
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