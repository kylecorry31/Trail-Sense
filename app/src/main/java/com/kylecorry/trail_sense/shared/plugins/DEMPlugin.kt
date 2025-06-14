package com.kylecorry.trail_sense.shared.plugins

import android.content.Context
import android.os.IBinder
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense_dem.aidl.DEMService

class DEMPlugin(context: Context) : PluginService<DEMService>(
    context,
    "com.kylecorry.trail_sense_dem",
    "com.kylecorry.trail_sense_dem.DEM_SERVICE"
) {
    override fun getServiceInterface(binder: IBinder?): DEMService {
        return DEMService.Stub.asInterface(binder)
    }

    suspend fun getElevation(coordinate: Coordinate): Float? = onIO {
        waitUntilConnected()
        service?.getElevation(coordinate.latitude, coordinate.longitude)?.elevationMeters
    }
}
