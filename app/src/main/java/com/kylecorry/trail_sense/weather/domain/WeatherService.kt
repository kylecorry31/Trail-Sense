package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.trail_sense.weather.domain.forcasting.DailyForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.HourlyForecaster
import com.kylecorry.trail_sense.weather.domain.sealevel.AltimeterSeaLevelPressureConverter
import com.kylecorry.trail_sense.weather.domain.sealevel.BarometerGPSAltitudeCalculator
import com.kylecorry.trail_sense.weather.domain.sealevel.GPSAltitudeCalculator
import com.kylecorry.trail_sense.weather.domain.tendency.DropPressureTendencyCalculator
import com.kylecorry.trail_sense.weather.domain.tendency.PressureTendency
import com.kylecorry.trailsensecore.domain.weather.*
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import java.time.Instant

class WeatherService(
    stormThreshold: Float,
    dailyForecastChangeThreshold: Float,
    hourlyForecastChangeThreshold: Float,
    adjustSeaLevelWithBarometer: Boolean = true,
    adjustSeaLevelWithTemp: Boolean = false
) {

    private val shortTermForecaster =
        HourlyForecaster(stormThreshold, hourlyForecastChangeThreshold)
    private val longTermForecaster = DailyForecaster(dailyForecastChangeThreshold)
    private val tendencyCalculator = DropPressureTendencyCalculator()
    private val newWeatherService: IWeatherService = WeatherService()
    private val seaLevelConverter = AltimeterSeaLevelPressureConverter(
        if (adjustSeaLevelWithBarometer) BarometerGPSAltitudeCalculator() else GPSAltitudeCalculator(),
        adjustSeaLevelWithTemp
    )

    fun getHourlyWeather(readings: List<PressureReading>): Weather {
        return shortTermForecaster.forecast(readings)
    }

    fun getDailyWeather(readings: List<PressureReading>): Weather {
        return longTermForecaster.forecast(readings)
    }

    fun getTendency(readings: List<PressureReading>): PressureTendency {
        return tendencyCalculator.calculate(readings)
    }

    fun classifyPressure(pressure: Float): PressureClassification {
        return newWeatherService.classifyPressure(PressureReading(Instant.now(), pressure))
    }

    fun convertToSeaLevel(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return seaLevelConverter.convert(readings)
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

    fun getHumidityComfortLevel(dewPoint: Float): HumidityComfortLevel {
        return newWeatherService.getHumidityComfortLevel(dewPoint)
    }


}