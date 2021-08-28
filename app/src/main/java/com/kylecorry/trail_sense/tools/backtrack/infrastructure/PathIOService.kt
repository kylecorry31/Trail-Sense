package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import com.kylecorry.andromeda.gpx.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class PathIOService(private val context: Context) {

    fun toGPX(path: List<PathPoint>): String {
        val waypoints = path.map {
            GPXWaypoint(it.coordinate, null, it.elevation, null, it.time, null)
        }
        val pathId = path.firstOrNull()?.pathId ?: 0

        val trackSegment = GPXTrackSegment(waypoints)
        val track = GPXTrack(null, null, pathId, null, listOf(trackSegment))

        return GPXParser.toGPX(
            GPXData(emptyList(), listOf(track)),
            context.getString(R.string.app_name)
        )
    }

}