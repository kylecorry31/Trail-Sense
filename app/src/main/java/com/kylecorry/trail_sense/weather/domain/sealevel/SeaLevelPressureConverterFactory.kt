package com.kylecorry.trail_sense.weather.domain.sealevel

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R

class SeaLevelPressureConverterFactory {

    fun create(context: Context): ISeaLevelPressureConverter {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val useSeaLevel = prefs.getBoolean(context.getString(R.string.pref_use_sea_level_pressure), false)

        if (!useSeaLevel){
            return NullPressureConverter()
        }

        val mode = prefs.getString(context.getString(R.string.pref_barometer_mode), "barometer_gps")

        return if (mode == "barometer_gps"){
            AltimeterSeaLevelPressureConverter(BarometerGPSAltitudeCalculator())
        } else {
            AltimeterSeaLevelPressureConverter(GPSAltitudeCalculator())
        }
    }

}