package com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues

import android.content.Context
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class CellSignalPointValueStrategy(private val context: Context) : IPointValueStrategy {
    override fun getValue(point: PathPoint): String {
        val signal = point.cellSignal ?: return ""
        val network = signal.network
        return FormatService(context).formatCellNetwork(network)
    }
}