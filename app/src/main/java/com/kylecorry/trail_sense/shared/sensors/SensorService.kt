package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences

class SensorService(private val context: Context) {

    private val userPrefs = UserPreferences(context)

    fun getGPS(): IGPS {
       return if (userPrefs.useLocationFeatures) GPS(context) else FakeGPS(context)
    }

    fun getAltimeter(existingGps: IGPS? = null): IAltimeter {
        return if (userPrefs.useAutoAltitude && userPrefs.useLocationFeatures){
            if (userPrefs.weather.hasBarometer && userPrefs.useFineTuneAltitude) {
                FusedAltimeter(if (existingGps is GPS) existingGps else GPS(context), Barometer(context))
            } else {
                if (existingGps is GPS) existingGps else GPS(context)
            }
        } else if (userPrefs.useAutoAltitude && userPrefs.weather.hasBarometer){
            Barometer(context)
        } else {
            FakeGPS(context)
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