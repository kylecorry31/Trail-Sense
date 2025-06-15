package com.kylecorry.trail_sense.shared.plugins

import android.content.Context
import android.os.IBinder
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense_dem.aidl.IDigitalElevationModelService

class DEMPlugin(context: Context) : PluginService<IDigitalElevationModelService>(
    context,
    "com.kylecorry.trail_sense_dem",
    "com.kylecorry.trail_sense_dem.DEM_SERVICE"
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

    companion object {
        fun isInstalled(context: Context): Boolean {
            return Package.isPackageInstalled(context, "com.kylecorry.trail_sense_dem")
        }
    }
}
