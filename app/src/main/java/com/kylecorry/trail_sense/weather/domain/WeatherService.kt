package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.*
import com.kylecorry.sol.science.meteorology.WeatherService
import com.kylecorry.sol.science.meteorology.forecast.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.forcasting.DailyForecaster
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import java.time.Duration
import java.time.Instant

class WeatherService(
    private val stormThreshold: Float,
    dailyForecastChangeThreshold: Float,
    private val hourlyForecastChangeThreshold: Float
) {
    private val longTermForecaster = DailyForecaster(dailyForecastChangeThreshold)
    private val newWeatherService: IWeatherService = WeatherService()


    fun getHourlyWeather(readings: List<PressureReading>): Weather {
        val tendency = getTendency(readings)
        return newWeatherService.forecast(tendency, stormThreshold)
    }

    fun getDailyWeather(readings: List<PressureReading>): Weather {
        return longTermForecaster.forecast(readings)
    }

    fun getTendency(readings: List<PressureReading>): PressureTendency {
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
            Pressure.hpa(last.value),
            Pressure.hpa(current.value),
            Duration.between(last.time, current.time),
            hourlyForecastChangeThreshold / 3f
        )

        return tendency.copy(amount = tendency.amount * 3f)
    }

    fun calibrate(
        readings: List<PressureAltitudeReading>,
        prefs: UserPreferences
    ): List<PressureReading> {
        val calibrationStrategy = SeaLevelCalibrationFactory().create(prefs)
        return calibrationStrategy.calibrate(readings)
    }

    fun getHeatIndex(tempCelsius: Float, relativeHumidity: Float): Float {
        return newWeatherService.getHeatIndex(tempCelsius, relativeHumidity)
    }

    fun getHeatAlert(heatIndex: Float): HeatAlert {
        return newWeatherService.getHeatAlert(heatIndex)
    }

    fun getDewPoint(tempCelsius: Float, relativeHumidity: Float): Float {
        return newWeatherService.getDewPoint(tempCelsius, relativeHumidity)
    }
}