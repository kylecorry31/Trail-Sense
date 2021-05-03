package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.domain.sealevel.NullPressureConverter
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureReading

object PressureCalibrationUtils {


    fun calibratePressures(context: Context, readings: List<PressureAltitudeReading>, useSeaLevel: Boolean? = null): List<PressureReading> {
        val prefs = UserPreferences(context)
        return if (useSeaLevel ?: prefs.weather.useSeaLevelPressure) {
            getSeaLevelPressureHistory(prefs, readings)
        } else {
            getPressureHistory(readings)
        }
    }

    private fun getSeaLevelPressureHistory(prefs: UserPreferences, readings: List<PressureAltitudeReading>): List<PressureReading> {
        val weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
        return weatherService.convertToSeaLevel(
            readings, prefs.weather.requireDwell, prefs.weather.maxNonTravellingAltitudeChange,
            prefs.weather.maxNonTravellingPressureChange,
            prefs.weather.experimentalConverter,
            prefs.altimeterMode == UserPreferences.AltimeterMode.Override
        )
    }

    private fun getPressureHistory(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return NullPressureConverter().convert(readings)
    }

}