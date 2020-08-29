package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS

class SensorService(private val context: Context) {

    private val userPrefs = UserPreferences(context)

    fun getGPS(): IGPS {
        if (!userPrefs.useAutoLocation){
            return OverrideGPS(context)
        }

        if (userPrefs.useLocationFeatures){
            return GPS(context)
        }

        return CachedGPS(context)
    }

    fun getAltimeter(existingGps: IGPS? = null): IAltimeter {
        if (!userPrefs.useAutoAltitude){
            return OverrideAltimeter(context)
        }

        if (!userPrefs.useLocationFeatures){
            return CachedAltimeter(context)
        }

        val gps = if (existingGps is GPS) existingGps else getGPS()

        return if (userPrefs.useFineTuneAltitude && userPrefs.weather.hasBarometer){
            FusedAltimeter(gps, Barometer(context))
        } else {
            gps
        }
    }

    fun getCompass(): ICompass {
        return if (userPrefs.navigation.useLegacyCompass) LegacyCompass(context) else VectorCompass(context)
    }

    fun getDeviceOrientation(): DeviceOrientation {
        return DeviceOrientation(context)
    }

    fun getBarometer(): IBarometer {
        return if (userPrefs.weather.hasBarometer) Barometer(context) else NullBarometer()
    }

}