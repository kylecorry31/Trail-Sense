package com.kylecorry.trail_sense.plugin.sample.service

import android.content.Context
import android.os.IBinder
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.plugin.sample.SamplePluginRegistration
import com.kylecorry.trail_sense.plugin.sample.aidl.ISampleOnePluginService
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugins.plugins.PluginServiceConnection
import com.kylecorry.trail_sense.plugins.plugins.Plugins

class SampleOnePluginService(context: Context) : PluginServiceConnection<ISampleOnePluginService>(
    context,
    Plugins.PLUGIN_SAMPLE,
    SamplePluginRegistration.PLUGIN_SERVICE_ID_SAMPLE_ONE_SERVICE
) {
    override fun getServiceInterface(binder: IBinder?): ISampleOnePluginService {
        return ISampleOnePluginService.Stub.asInterface(binder)
    }

    suspend fun getWeather(location: Coordinate): Forecast? = onIO {
        // TODO: Only invoke this if the service has location permission
        val json =
            service?.getWeather(location.latitude.roundPlaces(2), location.longitude.roundPlaces(2))
                ?: return@onIO null
        JsonConvert.fromJson<Forecast>(json)
    }
}