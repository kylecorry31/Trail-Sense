package com.kylecorry.trail_sense.navigation.beacons.infrastructure.export

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXWaypoint
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup

class BeaconGpxConverter {

    fun toGPX(beacons: List<Beacon>, groups: List<BeaconGroup>): GPXData {
        val groupNames = mutableMapOf<Long, String>()
        for (group in groups) {
            groupNames[group.id] = group.name
        }

        val waypoints = beacons.map {
            GPXWaypoint(
                it.coordinate,
                it.name,
                it.elevation,
                it.comment,
                null,
                if (it.beaconGroupId == null) null else groupNames[it.beaconGroupId]
            )
        }

        return GPXData(waypoints, emptyList())
    }

}