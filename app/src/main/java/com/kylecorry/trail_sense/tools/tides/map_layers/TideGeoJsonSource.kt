package com.kylecorry.trail_sense.tools.tides.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadAllTideTablesCommand
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator

class TideGeoJsonSource : GeoJsonSource {

    var showNearbyTides: Boolean = false

    override suspend fun load(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): GeoJsonObject {
        val context = AppServiceRegistry.get<Context>()
        val tideService = TideService(context)
        val tables = LoadAllTideTablesCommand(context).execute() + getNearbyTideTables(
            bounds,
            metersPerPixel
        )
        val currentTideCommand = CurrentTideTypeCommand(tideService)
        val tides =
            tables.filter {
                if (!it.isVisible) {
                    return@filter false
                }

                if (it.location != null) {
                    return@filter true
                }

                showNearbyTides && it.isAutomaticNearbyTide
            }

        val features = Parallel.map(tides) { table ->
            val type = currentTideCommand.execute(table)
            val location = tideService.getLocation(table) ?: return@map null
            val icon = when (type) {
                TideType.High -> BeaconIcon.TideHigh
                TideType.Low -> BeaconIcon.TideLow
                null -> BeaconIcon.TideHalf
            }

            GeoJsonFeature.point(
                location,
                icon = icon.id,
                iconSize = 12f,
                markerShape = null
            )
        }

        return GeoJsonFeatureCollection(features.filterNotNull())
    }

    private fun getNearbyTideTables(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): List<TideTable> {
        if (!showNearbyTides) {
            return emptyList()
        }

        val zoom = TileMath.getZoomLevel(bounds, metersPerPixel)

        if (zoom < 13){
            return emptyList()
        }

        // TODO: Resolution based on meters per pixel
        val grid = bounds.grid(2.0)
        return grid.map {
            TideTable(
                -2,
                emptyList(),
                null,
                estimator = TideEstimator.TideModel,
                isEditable = false,
                location = it
            )
        }
    }

    fun CoordinateBounds.grid(resolution: Double): List<Coordinate> {
        val latitudes = Interpolation.getMultiplesBetween(
            south - resolution,
            north + resolution,
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween(
            west - resolution,
            (if (west < east) east else east + 360) + resolution,
            resolution
        )

        val points = mutableListOf<Coordinate>()
        for (lat in latitudes) {
            for (lon in longitudes) {
                points.add(Coordinate(lat, lon))
            }
        }
        return points
    }
}
