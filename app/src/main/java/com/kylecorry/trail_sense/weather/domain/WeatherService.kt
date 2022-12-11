package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences

class WeatherService(prefs: WeatherPreferences) {
    private val stormThreshold = prefs.stormAlertThreshold
    private val hourlyForecastChangeThreshold = prefs.hourlyForecastChangeThreshold

    fun getForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        temperatureRange: Range<Temperature>?
    ): List<WeatherForecast> {
        return Meteorology.forecast(
            pressures,
            clouds,
            temperatureRange,
            hourlyForecastChangeThreshold / 3f,
            stormThreshold / 3f
        )
    }
}