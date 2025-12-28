package com.kylecorry.trail_sense.tools.tides.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.map.infrastructure.LandModel
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadAllTideTablesCommand
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator

class TideGeoJsonSource : GeoJsonSource {

    var showNearbyTides: Boolean = false
    private val minZoomLevel = 5
    private val maxDistanceFromTideModel = Distance.kilometers(100f).meters().value

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
            tables.filter { it.location != null && it.isVisible }

        val features = Parallel.map(tides) { table ->
            val type = currentTideCommand.execute(table)
            val location = table.location ?: return@map null
            val calculatedLocation = tideService.getLocation(table) ?: return@map null
            if (location != calculatedLocation && location.distanceTo(calculatedLocation) > maxDistanceFromTideModel) {
                return@map null
            }
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

    private suspend fun getNearbyTideTables(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): List<TideTable> {
        if (!showNearbyTides) {
            return emptyList()
        }

        val zoom = TileMath.getZoomLevel(bounds, metersPerPixel)
        if (zoom < minZoomLevel) {
            return emptyList()
        }

        // TODO: Limit max locations?
        val coastal = LandModel.getCoastalLocations(AppServiceRegistry.get(), bounds)
        return coastal.map {
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
