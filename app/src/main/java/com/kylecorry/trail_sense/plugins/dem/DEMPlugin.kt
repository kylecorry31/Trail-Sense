package com.kylecorry.trail_sense.plugins.dem

import android.content.Context
import android.os.IBinder
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.plugins.plugins.PluginServiceConnection
import com.kylecorry.trail_sense.plugins.plugins.Plugins
import com.kylecorry.trail_sense_dem.aidl.IDigitalElevationModelService

class DEMPlugin(context: Context) : PluginServiceConnection<IDigitalElevationModelService>(
    context,
    Plugins.DIGITAL_ELEVATION_MODEL,
    DEMPluginRegistration.SERVICE_DEM
) {
    override fun getServiceInterface(binder: IBinder?): IDigitalElevationModelService {
        return IDigitalElevationModelService.Stub.asInterface(binder)
    }

    suspend fun getElevation(coordinate: Coordinate): Float? = onIO {
        waitUntilConnected()
        val result = service?.getElevation(coordinate.latitude, coordinate.longitude)
        if (result?.isNaN() == true) {
            null
        } else {
            result
        }
    }
}