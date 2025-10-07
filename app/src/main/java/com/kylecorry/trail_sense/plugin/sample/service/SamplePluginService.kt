package com.kylecorry.trail_sense.plugin.sample.service

import android.content.Context
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.domain.WeatherRequest
import com.kylecorry.trail_sense.plugins.plugins.IpcServicePlugin
import com.kylecorry.trail_sense.plugins.plugins.Plugins
import com.kylecorry.trail_sense.plugins.plugins.fromJson
import com.kylecorry.trail_sense.plugins.plugins.toJsonBytes

class SamplePluginService(
    context: Context
) : IpcServicePlugin(Plugins.getPackageId(context, Plugins.PLUGIN_SAMPLE), context) {

    suspend fun ping(): String? {
        return send("/ping")?.toString(Charsets.UTF_8)
    }

    suspend fun getWeather(location: Coordinate): Forecast? {
        return send(
            "/weather", WeatherRequest(
                location.latitude.roundPlaces(2),
                location.longitude.roundPlaces(2)
            ).toJsonBytes()
        )?.fromJson()
    }
}