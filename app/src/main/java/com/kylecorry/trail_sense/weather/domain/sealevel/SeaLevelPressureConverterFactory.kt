package com.kylecorry.trail_sense.weather.domain.sealevel

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences

class SeaLevelPressureConverterFactory {

    fun create(context: Context): ISeaLevelPressureConverter {
        val prefs = UserPreferences(context)

        if (!prefs.weather.useSeaLevelPressure){
            return NullPressureConverter()
        }

        return AltimeterSeaLevelPressureConverter(BarometerGPSAltitudeCalculator())
    }

}