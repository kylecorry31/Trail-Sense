package com.kylecorry.trail_sense.navigation.paths.domain

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXTrack
import com.kylecorry.andromeda.gpx.GPXTrackSegment
import com.kylecorry.andromeda.gpx.GPXWaypoint

class PathGPXConverter {

    fun toGPX(name: String?, path: List<PathPoint>): GPXData {
        val waypoints = path.map {
            GPXWaypoint(it.coordinate, elevation = it.elevation, time = it.time)
        }
        val pathId = path.firstOrNull()?.pathId ?: 0

        val trackSegment = GPXTrackSegment(waypoints)
        val track = GPXTrack(name, id = pathId, segments = listOf(trackSegment))
        return GPXData(emptyList(), listOf(track), emptyList())
    }

}