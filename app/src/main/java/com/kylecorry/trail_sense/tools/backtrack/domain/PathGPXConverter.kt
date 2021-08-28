package com.kylecorry.trail_sense.tools.backtrack.domain

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXTrack
import com.kylecorry.andromeda.gpx.GPXTrackSegment
import com.kylecorry.andromeda.gpx.GPXWaypoint
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class PathGPXConverter {

    fun toGPX(path: List<PathPoint>): GPXData {
        val waypoints = path.map {
            GPXWaypoint(it.coordinate, null, it.elevation, null, it.time, null)
        }
        val pathId = path.firstOrNull()?.pathId ?: 0

        val trackSegment = GPXTrackSegment(waypoints)
        val track = GPXTrack(null, null, pathId, null, listOf(trackSegment))
        return GPXData(emptyList(), listOf(track))
    }

}