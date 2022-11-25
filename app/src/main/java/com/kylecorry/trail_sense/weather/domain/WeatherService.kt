package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences

class WeatherService(private val prefs: WeatherPreferences) {
    private val stormThreshold = prefs.stormAlertThreshold
    private val hourlyForecastChangeThreshold = prefs.hourlyForecastChangeThreshold

    fun getForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>
    ): List<WeatherForecast> {
        return Meteorology.forecast(
            pressures,
            clouds,
            hourlyForecastChangeThreshold / 3f,
            stormThreshold / 3f
        )
    }
}