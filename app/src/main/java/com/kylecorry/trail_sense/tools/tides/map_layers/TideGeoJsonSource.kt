package com.kylecorry.trail_sense.tools.tides.map_layers

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.map.infrastructure.LandModel
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTidePhaseCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadAllTideTablesCommand
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator

import java.time.Instant

class TideGeoJsonSource : GeoJsonSource {

    private val minZoomLevel = 8

    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject {
        val preferences = params.getBundle(GeoJsonSource.PARAM_PREFERENCES)
        val showModeledTides =
            preferences?.getBoolean(SHOW_MODELED_TIDES, DEFAULT_SHOW_MODELED_TIDES)
                ?: DEFAULT_SHOW_MODELED_TIDES
        val showPhase =
            preferences?.getBoolean(SHOW_PHASE, DEFAULT_SHOW_PHASE)
                ?: DEFAULT_SHOW_PHASE

        val time = Instant.ofEpochMilli(params.getLong(GeoJsonSource.PARAM_TIME))
        val context = AppServiceRegistry.get<Context>()
        val tideService = TideService(context)
        val tables = LoadAllTideTablesCommand(context).execute() + getNearbyTideTables(
            bounds,
            zoom,
            showModeledTides
        )
        val currentTideCommand = CurrentTideTypeCommand(tideService)
        val tidePhaseCommand = CurrentTidePhaseCommand(tideService)
        val tides =
            tables.filter { it.location != null && it.isVisible }

        val features = Parallel.map(tides) { table ->
            val location = table.location ?: return@map null

            val phase = if (showPhase) {
                tidePhaseCommand.execute(table, time) ?: return@map null
            } else {
                null
            }

            val icon = if (showPhase) {
                BeaconIcon.Arrow
            } else {
                when (currentTideCommand.execute(table, time)) {
                    TideType.High -> BeaconIcon.TideHigh
                    TideType.Low -> BeaconIcon.TideLow
                    null -> BeaconIcon.TideHalf
                }
            }

            GeoJsonFeature.point(
                location,
                icon = icon.id,
                iconColor = if (showPhase) {
                    Color.BLACK
                } else {
                    null
                },
                color = Color.WHITE,
                rotation = phase
            )
        }

        return GeoJsonFeatureCollection(features.filterNotNull())
    }

    private suspend fun getNearbyTideTables(
        bounds: CoordinateBounds,
        zoom: Int,
        showModeledTides: Boolean
    ): List<TideTable> {
        if (!showModeledTides) {
            return emptyList()
        }

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

    companion object {
        const val SOURCE_ID = "tide"
        const val SHOW_MODELED_TIDES = "show_modeled_tides"
        const val DEFAULT_SHOW_MODELED_TIDES = false
        const val SHOW_PHASE = "show_phase"
        const val DEFAULT_SHOW_PHASE = false
    }

}
