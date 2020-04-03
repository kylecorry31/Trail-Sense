package com.kylecorry.trail_sense.astronomy.sun

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R

class SunTimesCalculatorFactory {

    fun create(ctx: Context): ISunTimesCalculator {
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

        return when (prefs.getString(ctx.getString(R.string.pref_sun_time_mode), "actual")) {
            "civil" -> CivilTwilightCalculator()
            "nautical" -> NauticalTwilightCalculator()
            "astronomical" -> AstronomicalTwilightCalculator()
            else -> ActualTwilightCalculator()
        }
    }

    fun getAll(): List<ISunTimesCalculator> {
        return listOf(
            ActualTwilightCalculator(),
            CivilTwilightCalculator(),
            NauticalTwilightCalculator(),
            AstronomicalTwilightCalculator()
        )
    }

}