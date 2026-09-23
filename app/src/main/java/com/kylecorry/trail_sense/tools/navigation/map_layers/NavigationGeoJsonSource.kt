package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class NavigationGeoJsonSource : GeoJsonSource {

    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()
    private val navigator = DependencyRegistry.get<Navigator>()

    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject? {
        val myLocation = locationSubsystem.location
        val destination = navigator.getDestination2() ?: return null
        val paths = createPath(myLocation, destination)
        val showEndpoints = params.getPreferences().getBoolean(SHOW_ENDPOINTS, false)

        return GeoJsonFeatureCollection(paths.flatMap { createFeatures(it, showEndpoints) })
    }

    private fun createFeatures(path: MappablePath, showEndpoints: Boolean): List<GeoJsonFeature> {
        return listOfNotNull(
            GeoJsonFeature.lineString(
                path.points,
                path.id,
                lineStyle = path.style,
                color = path.color,
                thicknessScale = path.thicknessScale
            ),
            if (showEndpoints) path.points.firstOrNull()?.let {
                GeoJsonFeature.point(
                    it,
                    id = "navigation-start-${path.id}",
                    color = path.color,
                    strokeColor = Color.WHITE,
                    strokeWeight = 1f,
                    size = 8f
                )
            } else null,
            if (showEndpoints) path.points.lastOrNull()?.let {
                GeoJsonFeature.point(
                    it,
                    id = "navigation-end-${path.id}",
                    color = Color.WHITE,
                    icon = BeaconIcon.Flag.id,
                    iconColor = path.color,
                    strokeColor = path.color,
                    strokeWeight = 1f,
                    size = 12f,
                    iconSize = 8f
                )
            } else null
        )
    }

    private fun createPath(
        myLocation: Coordinate,
        destination: Destination
    ): List<MappablePath> {
        return when (destination) {
            is Destination.Path -> listOf(
                MappablePath(-1, destination.route.navigate(myLocation).remainingRoute,
                    destination.path.style.color, LineStyle.Arrow, 1.5f)
            )
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
        return if (bearing.startingLocation != null && prefs.navigation.lockBearingToLocation) {
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
                    myLocation.plus(bearing.bearingDistance, bearing.trueBearing),
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
        const val SHOW_ENDPOINTS = "show_endpoints"
    }
}
