package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.*
import com.kylecorry.sol.science.meteorology.WeatherService
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.forcasting.DailyForecaster
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences
import java.time.Duration
import java.time.Instant

class WeatherService(private val prefs: WeatherPreferences) {
    private val stormThreshold = prefs.stormAlertThreshold
    private val hourlyForecastChangeThreshold = prefs.hourlyForecastChangeThreshold
    private val longTermForecaster = DailyForecaster(prefs.dailyForecastChangeThreshold)
    private val newWeatherService: IWeatherService = WeatherService()

    fun calibrateTemperature(temp: Float): Float {
        val calibrated1 = prefs.minActualTemperature
        val uncalibrated1 = prefs.minBatteryTemperature
        val calibrated2 = prefs.maxActualTemperature
        val uncalibrated2 = prefs.maxBatteryTemperature

        return calibrated1 + (calibrated2 - calibrated1) * (uncalibrated1 - temp) / (uncalibrated1 - uncalibrated2)
    }

    fun getHourlyWeather(readings: List<Reading<Pressure>>): Weather {
        val tendency = getTendency(readings)
        return newWeatherService.forecast(tendency, stormThreshold)
    }

    fun getDailyWeather(readings: List<Reading<Pressure>>): Weather {
        return longTermForecaster.forecast(readings)
    }

    fun getTendency(readings: List<Reading<Pressure>>): PressureTendency {
        val last = readings.minByOrNull {
            Duration.between(
                it.time,
                Instant.now().minus(Duration.ofHours(3))
            ).abs()
        }
        val current = readings.lastOrNull()

        if (last == null || current == null) {
            return PressureTendency(PressureCharacteristic.Steady, 0f)
        }

        val tendency = newWeatherService.getTendency(
            last.value.hpa(),
            current.value.hpa(),
            Duration.between(last.time, current.time),
            hourlyForecastChangeThreshold / 3f
        )

        return tendency.copy(amount = tendency.amount * 3f)
    }

    fun calibrate(
        readings: List<Reading<RawWeatherObservation>>,
        prefs: UserPreferences
    ): List<Reading<Pressure>> {
        val calibrationStrategy = SeaLevelCalibrationFactory().create(prefs)
        return calibrationStrategy.calibrate(readings)
    }

}