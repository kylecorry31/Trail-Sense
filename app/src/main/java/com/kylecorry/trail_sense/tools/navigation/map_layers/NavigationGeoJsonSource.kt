package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.content.Context

import android.os.Bundle
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class NavigationGeoJsonSource : GeoJsonSource {

    var myLocation: Coordinate? = null
    var destination: Destination? = null
    var useLocationWithBearing: Boolean = true

    override suspend fun load(context: Context, bounds: CoordinateBounds, zoom: Int, params: Bundle): GeoJsonObject? {
        val myLocation = myLocation
        val destination = destination

        if (destination == null || myLocation == null) {
            return null
        }

        val paths = createPath(myLocation, destination)

        return GeoJsonFeatureCollection(paths.map {
            GeoJsonFeature.lineString(
                it.points,
                it.id,
                lineStyle = it.style,
                color = it.color,
                thicknessScale = it.thicknessScale
            )
        })
    }

    private fun createPath(
        myLocation: Coordinate,
        destination: Destination
    ): List<MappablePath> {
        return when (destination) {
            is Destination.Beacon -> createBeaconPath(myLocation, destination)
            is Destination.Bearing -> createBearingPath(myLocation, destination)
        }
    }

    private fun createBeaconPath(
        myLocation: Coordinate,
        beacon: Destination.Beacon
    ): List<MappablePath> {
        return listOf(
            createPath(myLocation, beacon.beacon.coordinate, beacon.beacon.color)
        )
    }

    private fun createBearingPath(
        myLocation: Coordinate,
        bearing: Destination.Bearing
    ): List<MappablePath> {
        return if (bearing.startingLocation != null && useLocationWithBearing) {
            listOf(
                createPath(
                    bearing.startingLocation,
                    bearing.targetLocation!!,
                    Destination.Bearing.defaultColor
                )
            )
        } else {
            listOf(
                createPath(
                    myLocation,
                    myLocation.plus(Destination.Bearing.bearingDistance, bearing.trueBearing),
                    Destination.Bearing.defaultColor
                )
            )
        }
    }

    private fun createPath(start: Coordinate, end: Coordinate, @ColorInt color: Int): MappablePath {
        return MappablePath(
            -1, listOf(start, end), color, LineStyle.Arrow, thicknessScale = 1.5f
        )
    }

    private data class MappablePath(
        val id: Long,
        val points: List<Coordinate>,
        @ColorInt val color: Int,
        val style: LineStyle,
        val thicknessScale: Float
    )

    companion object {
        const val SOURCE_ID = "navigation"
    }
}
