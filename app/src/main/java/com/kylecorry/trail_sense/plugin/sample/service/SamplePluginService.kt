package com.kylecorry.trail_sense.plugin.sample.service

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.domain.WeatherRequest
import com.kylecorry.trail_sense.plugins.plugins.IpcServicePlugin
import com.kylecorry.trail_sense.plugins.plugins.Plugins

class SamplePluginService(
    context: Context
) : IpcServicePlugin(Plugins.getPackageId(context, Plugins.PLUGIN_SAMPLE), context) {

    suspend fun ping(): String? {
        val response = send("/ping")
        return response?.toString(Charsets.UTF_8)
    }

    suspend fun getWeather(location: Coordinate): Forecast? {
        val payload = JsonConvert.toJson(
            WeatherRequest(
                location.latitude.roundPlaces(2),
                location.longitude.roundPlaces(2)
            )
        ).toByteArray()
        val response = send("/weather", payload)
        val json = response?.toString(Charsets.UTF_8) ?: return null
        return JsonConvert.fromJson<Forecast>(json)
    }
}