package com.kylecorry.trail_sense.plugins.map_layers

import android.Manifest
import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.geojson.GeoJsonConvert
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.infrastructure.PluginGuard
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.withTimeout
import java.io.ByteArrayInputStream
import java.time.Instant

class PluginGeoJsonSource(private val packageId: String, private val endpoint: String) : GeoJsonSource {
    private val plugins = getAppService<PluginSubsystem>()

    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject? {
        val connection = plugins.getPluginResourceServiceConnection(packageId)
        return connection?.use {
            val payload = Payload(
                bounds.north,
                bounds.south,
                bounds.east,
                bounds.west,
                zoom,
                params.getLong(MapLayerParams.PARAM_TIME, Instant.now().toEpochMilli())
            )
            val geojson =
                it.send(
                    endpoint,
                    payload,
                    requiredPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
                )?.payload
                    ?: return@use null
            if (!PluginGuard.isValidGeoJsonPayload(geojson)) {
                return@use null
            }

            try {
                val converted = withTimeout(PluginGuard.MAX_GEOJSON_PARSE_TIME) {
                    GeoJsonConvert.fromJson(ByteArrayInputStream(geojson))
                }
                if (converted != null && PluginGuard.isValidGeoJson(converted)){
                    converted
                } else {
                    null
                }
            } catch (_: TimeoutCancellationException) {
                null
            }
        }
    }

    private data class Payload(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double,
        val zoom: Int,
        val time: Long
    ) : ProguardIgnore
}
