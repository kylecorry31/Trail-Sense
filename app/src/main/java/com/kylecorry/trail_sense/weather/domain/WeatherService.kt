package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.trail_sense.weather.domain.classifier.PressureClassification
import com.kylecorry.trail_sense.weather.domain.classifier.StandardPressureClassifier
import com.kylecorry.trail_sense.weather.domain.forcasting.DailyForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.HourlyForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.Weather
import com.kylecorry.trail_sense.weather.domain.sealevel.AltimeterSeaLevelPressureConverter
import com.kylecorry.trail_sense.weather.domain.sealevel.BarometerGPSAltitudeCalculator
import com.kylecorry.trail_sense.weather.domain.sealevel.GPSAltitudeCalculator
import com.kylecorry.trail_sense.weather.domain.tendency.DropPressureTendencyCalculator
import com.kylecorry.trail_sense.weather.domain.tendency.PressureTendency

class WeatherService(
    stormThreshold: Float,
    dailyForecastChangeThreshold: Float,
    hourlyForecastChangeThreshold: Float
) {

    private val shortTermForecaster =
        HourlyForecaster(stormThreshold, hourlyForecastChangeThreshold)
    private val longTermForecaster = DailyForecaster(dailyForecastChangeThreshold)
    private val tendencyCalculator = DropPressureTendencyCalculator()
    private val pressureClassifier = StandardPressureClassifier()
    private val seaLevelConverter = AltimeterSeaLevelPressureConverter(
        GPSAltitudeCalculator()
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
        return pressureClassifier.classify(pressure)
    }

    fun convertToSeaLevel(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return seaLevelConverter.convert(readings)
    }

}