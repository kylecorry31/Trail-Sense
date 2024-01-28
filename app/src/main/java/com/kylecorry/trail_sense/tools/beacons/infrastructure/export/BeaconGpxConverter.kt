package com.kylecorry.trail_sense.tools.beacons.infrastructure.export

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXWaypoint
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup

class BeaconGpxConverter {

    fun toGPX(beacons: List<Beacon>, groups: List<BeaconGroup>): GPXData {
        val groupNames = mutableMapOf<Long, String>()
        for (group in groups) {
            groupNames[group.id] = group.name
        }

        val waypoints = beacons.map {
            GPXWaypoint(
                it.coordinate,
                name = it.name,
                elevation = it.elevation,
                comment = it.comment,
                group = if (it.parentId == null) null else groupNames[it.parentId]
            )
        }

        return GPXData(waypoints, emptyList(), emptyList())
    }

}