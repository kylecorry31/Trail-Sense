package com.kylecorry.trail_sense.tools.tides.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadAllTideTablesCommand

class TideGeoJsonSource : GeoJsonSource {

    override suspend fun load(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): GeoJsonObject? {
        val context = AppServiceRegistry.get<Context>()
        val tables = LoadAllTideTablesCommand(context).execute()
        val currentTideCommand = CurrentTideTypeCommand(TideService(context))
        val tides = tables.filter { it.location != null && it.isVisible }.map {
            it to currentTideCommand.execute(it)
        }

        val features = tides.mapNotNull { (table, type) ->
            val location = table.location ?: return@mapNotNull null
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

        return GeoJsonFeatureCollection(features)
    }
}
