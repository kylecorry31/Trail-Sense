package com.kylecorry.trail_sense.plugin.sample.service

import android.Manifest
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.domain.WeatherRequest
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.plugins.infrastructure.payloadAsJson

// Example of a community endpoint
class WeatherForecastService(
    private val plugin: PluginResourceServiceDetails,
    private val endpoint: String? = null
) {

    private val plugins = getAppService<PluginSubsystem>()

    suspend fun getWeather(location: Coordinate): Forecast? {

        val actualEndpoint = if (endpoint in plugin.features.weather) {
            endpoint
        } else {
            plugin.features.weather.firstOrNull()
        }

        if (actualEndpoint == null) {
            return null
        }

        // No permission
        if (!plugin.isOfficial) {
            return null
        }

        return plugins.callPluginEndpoint(
            plugin.packageId,
            actualEndpoint,
            WeatherRequest(
                location.latitude.roundPlaces(2),
                location.longitude.roundPlaces(2)
            ),
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        )?.payloadAsJson()
    }
}
