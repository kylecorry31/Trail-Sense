package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.cell

import android.content.Context
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService

internal class CellSignalLookup(
    context: Context
) {
    private val formatter = FormatService.getInstance(context)
    private val pathService = PathService.getInstance(context)

    private val maxDistance = Distance.miles(50f).meters().distance

    suspend fun getNearestCellSignalBeacon(location: Coordinate): Beacon? {
        val waypoints = pathService.getWaypointsWithCellSignal()
        val nearest = waypoints.minByOrNull {
            it.coordinate.distanceTo(location)
        }

        if (nearest != null && location.distanceTo(nearest.coordinate) >= maxDistance) {
            return null
        }

        return nearest?.let { createBeacon(it) }
    }


    private fun createBeacon(point: PathPoint): Beacon {
        return Beacon.temporary(
            point.coordinate,
            name = "${
                formatter.formatCellNetwork(
                    CellNetwork.entries
                        .first { it.id == point.cellSignal!!.network.id }
                )
            } (${formatter.formatQuality(point.cellSignal!!.quality)})",
            visible = false,
            elevation = point.elevation
        )
    }
}