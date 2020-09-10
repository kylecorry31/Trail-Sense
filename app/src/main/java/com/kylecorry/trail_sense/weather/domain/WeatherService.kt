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
import kotlin.math.abs
import kotlin.math.ln

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
    private val pressureClassifier = StandardPressureClassifier()
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
        return pressureClassifier.classify(pressure)
    }

    fun convertToSeaLevel(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return seaLevelConverter.convert(readings)
    }

    fun getHeatIndex(tempCelsius: Float, relativeHumidity: Float): Float {

        if (celsiusToFahrenheit(tempCelsius) < 80) return tempCelsius

        val c1 = -8.78469475556
        val c2 = 1.61139411
        val c3 = 2.33854883889
        val c4 = -0.14611605
        val c5 = -0.012308094
        val c6 = -0.0164248277778
        val c7 = 0.002211732
        val c8 = 0.00072546
        val c9 = -0.000003582

        val hi = c1 +
                c2 * tempCelsius +
                c3 * relativeHumidity +
                c4 * tempCelsius * relativeHumidity +
                c5 * tempCelsius * tempCelsius +
                c6 * relativeHumidity * relativeHumidity +
                c7 * tempCelsius * tempCelsius * relativeHumidity +
                c8 * tempCelsius * relativeHumidity * relativeHumidity +
                c9 * tempCelsius * tempCelsius * relativeHumidity * relativeHumidity

        return hi.toFloat()
    }

    fun getHeatAlert(heatIndex: Float): HeatAlert {
        return when {
            heatIndex <= -30 -> HeatAlert.FrostbiteDanger
            heatIndex <= -10 -> HeatAlert.FrostbiteWarning
            heatIndex <= 0 -> HeatAlert.FrostbiteCaution
            heatIndex < 80 -> HeatAlert.Normal
            heatIndex <= 90 -> HeatAlert.HeatCaution
            heatIndex <= 103 -> HeatAlert.HeatWarning
            heatIndex <= 125 -> HeatAlert.HeatAlert
            else -> HeatAlert.HeatDanger
        }
    }

    fun getDewPoint(tempCelsius: Float, relativeHumidity: Float): Float {
        val m = 17.62
        val tn = 243.12
        var lnRH = ln(relativeHumidity.toDouble() / 100)
        if (lnRH.isNaN() || abs(lnRH).isInfinite()) lnRH = ln(0.00001)
        val tempCalc = m * tempCelsius / (tn + tempCelsius)
        val top = lnRH + tempCalc
        var bottom = m - top
        if (bottom == 0.0) bottom = 0.00001
        val dewPoint = tn * top / bottom
        return dewPoint.toFloat()
    }

    fun celsiusToFahrenheit(tempCelsius: Float): Float {
        return tempCelsius * 9 / 5f + 32
    }

    fun getHumidityComfortLevel(dewPoint: Float): HumidityComfortLevel {
        return when {
            dewPoint <= 55 -> HumidityComfortLevel.Pleasant
            dewPoint <= 60 -> HumidityComfortLevel.Comfortable
            dewPoint <= 65 -> HumidityComfortLevel.Sticky
            dewPoint <= 70 -> HumidityComfortLevel.Uncomfortable
            dewPoint <= 75 -> HumidityComfortLevel.Oppressive
            else -> HumidityComfortLevel.Miserable
        }
    }


}