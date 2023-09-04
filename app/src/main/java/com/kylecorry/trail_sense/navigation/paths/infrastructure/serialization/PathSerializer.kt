package com.kylecorry.trail_sense.navigation.paths.infrastructure.serialization

import com.kylecorry.andromeda.core.io.ISerializer
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXRoute
import com.kylecorry.andromeda.gpx.GPXSerializer
import com.kylecorry.andromeda.gpx.GPXTrack
import com.kylecorry.andromeda.gpx.GPXTrackSegment
import com.kylecorry.andromeda.gpx.GPXWaypoint
import com.kylecorry.trail_sense.navigation.paths.domain.FullPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathMetadata
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.PathStyle
import java.io.InputStream
import java.io.OutputStream

class PathSerializer(private val defaultImportStyle: PathStyle = PathStyle.default()) :
    ISerializer<List<FullPath>> {

    private val gpxSerializer = GPXSerializer("Trail Sense")

    override fun deserialize(stream: InputStream): List<FullPath> {
        val gpx = gpxSerializer.deserialize(stream)
        val paths = mutableListOf<FullPath>()

        gpx.tracks.forEach {
            paths.add(trackToPath(it))
        }

        gpx.routes.forEach {
            paths.add(routeToPath(it))
        }

        return paths
    }

    override fun serialize(paths: List<FullPath>, stream: OutputStream) {
        val tracks = paths.map { (path, points, parent) ->
            val waypoints = points.map {
                GPXWaypoint(it.coordinate, elevation = it.elevation, time = it.time)
            }
            val pathId = points.firstOrNull()?.pathId ?: 0

            val trackSegment = GPXTrackSegment(waypoints)
            GPXTrack(
                path.name, id = pathId, segments = listOf(trackSegment), group = parent?.name
            )
        }

        val gpx = GPXData(emptyList(), tracks, emptyList())

        gpxSerializer.serialize(gpx, stream)
    }

    private fun routeToPath(route: GPXRoute): FullPath {
        return FullPath(Path(0, route.name, defaultImportStyle, PathMetadata.empty),
            route.points.map {
                PathPoint(0, 0, it.coordinate, it.elevation, it.time)
            })
    }

    private fun trackToPath(track: GPXTrack): FullPath {
        val points = mutableListOf<PathPoint>()
        for (segment in track.segments) {
            points.addAll(segment.points.map {
                PathPoint(0, 0, it.coordinate, it.elevation, it.time)
            })
        }
        return FullPath(
            Path(0, track.name, defaultImportStyle, PathMetadata.empty), points
        )
    }
}