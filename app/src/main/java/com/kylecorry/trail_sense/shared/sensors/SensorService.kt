package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences

class SensorService(private val context: Context) {

    private val userPrefs = UserPreferences(context)

    fun getGPS(): IGPS {
        // TODO:
        // If !location auto return override location
        // If location enabled, return GPS
        // Else return GPS cache / override location
       return if (userPrefs.useLocationFeatures) GPS(context) else FakeGPS(context)
    }

    fun getAltimeter(existingGps: IGPS? = null): IAltimeter {
        if (!userPrefs.useAutoAltitude){
            return FakeGPS(context) // TODO: Replace with override altimeter
        }

        if (!userPrefs.useLocationFeatures){
            return FakeGPS(context) // TODO: Replace with altimeter cache then override
        }

        val gps = if (existingGps is GPS) existingGps else GPS(context)

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