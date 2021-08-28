package com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class CellSignalPointColoringStrategy(private val colorScale: IColorScale) :
    IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int? {
        val pct = when (point.cellSignal?.quality) {
            Quality.Poor -> 0f
            Quality.Moderate -> 0.5f
            Quality.Good -> 1f
            else -> return null
        }
        return colorScale.getColor(pct)
    }
}