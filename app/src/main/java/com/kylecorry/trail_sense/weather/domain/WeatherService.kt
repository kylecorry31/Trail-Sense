package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences

class WeatherService(private val prefs: WeatherPreferences) {
    private val stormThreshold = prefs.stormAlertThreshold
    private val hourlyForecastChangeThreshold = prefs.hourlyForecastChangeThreshold

    fun calibrateTemperature(temp: Float): Float {
        val calibrated1 = prefs.minActualTemperature
        val uncalibrated1 = prefs.minBatteryTemperature
        val calibrated2 = prefs.maxActualTemperature
        val uncalibrated2 = prefs.maxBatteryTemperature

        return calibrated1 + (calibrated2 - calibrated1) * (uncalibrated1 - temp) / (uncalibrated1 - uncalibrated2)
    }

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

    fun calibrate(
        readings: List<Reading<RawWeatherObservation>>,
        prefs: UserPreferences
    ): List<Reading<Pressure>> {
        val calibrationStrategy = SeaLevelCalibrationFactory().create(prefs)
        return calibrationStrategy.calibrate(readings)
    }

}