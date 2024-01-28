package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXTrack
import com.kylecorry.andromeda.gpx.GPXTrackSegment
import com.kylecorry.andromeda.gpx.GPXWaypoint

class PathGPXConverter {

    fun toGPX(paths: List<FullPath>): GPXData {
        val tracks = paths.map { toGPX(it) }
        return GPXData(emptyList(), tracks.flatMap { it.tracks }, emptyList())
    }

    private fun toGPX(path: FullPath): GPXData {
        val waypoints = path.points.map {
            GPXWaypoint(it.coordinate, elevation = it.elevation, time = it.time)
        }
        val pathId = path.points.firstOrNull()?.pathId ?: 0

        val trackSegment = GPXTrackSegment(waypoints)
        val track = GPXTrack(
            path.path.name,
            id = pathId,
            segments = listOf(trackSegment),
            group = path.parent?.name
        )
        return GPXData(emptyList(), listOf(track), emptyList())
    }

}