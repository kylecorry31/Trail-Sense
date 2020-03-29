package com.kylecorry.trail_sense.weather.altimeter

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.Constants
import com.kylecorry.trail_sense.R

class AltitudeCalculatorFactory(private val context: Context) {

    fun create(): IAltitudeCalculator {

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = prefs.getString(context.getString(R.string.pref_barometer_mode), Constants.ALTIMETER_MODE_BAROMETER_GPS)

        return when (mode){
            Constants.ALTIMETER_MODE_GPS -> GPSAltitudeCalculator()
            Constants.ALTIMETER_MODE_BAROMETER_GPS -> BarometerGPSAltitudeCalculator(
                Constants.MAXIMUM_NATURAL_PRESSURE_CHANGE)
            else -> GPSAltitudeCalculator()
        }
    }

}