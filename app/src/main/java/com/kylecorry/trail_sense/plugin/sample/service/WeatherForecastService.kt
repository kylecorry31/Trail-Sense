package com.kylecorry.trail_sense.plugin.sample.service

import android.Manifest
import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.domain.WeatherRequest
import com.kylecorry.trail_sense.plugins.plugins.PluginResourceService
import com.kylecorry.trail_sense.plugins.plugins.ipcSend
import com.kylecorry.trail_sense.plugins.plugins.payloadAsJson

// Example of a community endpoint
class WeatherForecastService(
    private val context: Context,
    private val plugin: PluginResourceService,
    private val endpoint: String? = null
) {

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
        if (!plugin.isOfficial && !Permissions.hasPermission(
                context,
                plugin.packageId,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            return null
        }

        val response = ipcSend(
            context,
            plugin.packageId,
            actualEndpoint,
            WeatherRequest(
                location.latitude.roundPlaces(2),
                location.longitude.roundPlaces(2)
            )
        )

        return response.payloadAsJson()
    }
}