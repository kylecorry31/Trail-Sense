package com.kylecorry.trail_sense.tools.tides.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.shared.andromeda_temp.grid
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
    private val minZoomLevel = 10
    private val maxZoomLevel = 19
    private val baseResolution = 1 / 2.0
    private val validResolutions = mapOf(
        10 to baseResolution * 8,
        11 to baseResolution * 4,
        12 to baseResolution * 2,
        13 to baseResolution,
        14 to baseResolution,
        15 to baseResolution,
        16 to baseResolution,
        17 to baseResolution,
        18 to baseResolution,
        19 to baseResolution
    )

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

        val zoom = TileMath.getZoomLevel(bounds, metersPerPixel).coerceAtMost(maxZoomLevel)

        if (zoom < minZoomLevel) {
            return emptyList()
        }

        // TODO: Get the location of each coastal pixel in the area instead and apply a filter to that
        val grid = bounds.grid(validResolutions[zoom] ?: 10.0)
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


}
