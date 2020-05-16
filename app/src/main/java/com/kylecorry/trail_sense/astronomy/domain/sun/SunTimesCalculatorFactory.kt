package com.kylecorry.trail_sense.astronomy.domain.sun

import android.content.Context
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.shared.UserPreferences

class SunTimesCalculatorFactory {

    fun create(ctx: Context): ISunTimesCalculator {
        val prefs = UserPreferences(ctx)

        return when (prefs.astronomy.sunTimesMode) {
            AstronomyPreferences.SunTimesMode.Actual -> ActualTwilightCalculator()
            AstronomyPreferences.SunTimesMode.Civil -> CivilTwilightCalculator()
            AstronomyPreferences.SunTimesMode.Nautical -> NauticalTwilightCalculator()
            AstronomyPreferences.SunTimesMode.Astronomical -> AstronomicalTwilightCalculator()
        }
    }
}