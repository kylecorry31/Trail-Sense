package com.kylecorry.trail_sense.tools.backtrack.domain

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class CellSignalPointColoringStrategy : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int {
        return CustomUiUtils.getQualityColor(point.cellSignal?.quality ?: Quality.Unknown)
    }
}