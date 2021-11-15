package com.kylecorry.trail_sense.navigation.paths.domain

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXTrack
import com.kylecorry.andromeda.gpx.GPXTrackSegment
import com.kylecorry.andromeda.gpx.GPXWaypoint
import com.kylecorry.trail_sense.shared.paths.PathPoint

class PathGPXConverter {

    fun toGPX(name: String?, path: List<PathPoint>): GPXData {
        val waypoints = path.map {
            GPXWaypoint(it.coordinate, null, it.elevation, null, it.time, null)
        }
        val pathId = path.firstOrNull()?.pathId ?: 0

        val trackSegment = GPXTrackSegment(waypoints)
        val track = GPXTrack(name, null, pathId, null, listOf(trackSegment))
        return GPXData(emptyList(), listOf(track))
    }

}