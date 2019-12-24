package com.kylecorry.survival_aid.weather

import kotlin.math.abs
import kotlin.math.ln

/**
 * A collection of weather utilities
 */
object WeatherUtils {

    /**
     * Get the heat index in celsius
     * @param tempCelsius The temperature in celsius
     * @param relativeHumidity The relative humidity in percent (0 - 100)
     * @return The heat index in celsius
     */
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


    enum class HeatAlert(val readableName: String) {
        // TODO: Include freezing/frostbite alerts (https://www.weather.gov/bou/windchill)
        NORMAL("No alert"),
        CAUTION("Heat caution"),
        EXTREME_CAUTION("Heat warning"),
        DANGER("Heat alert"),
        EXTREME_DANGER("Extreme heat alert")
    }

    /**
     * Get the heat alert level
     * @param tempCelsius The temperature in celsius
     * @param relativeHumidity The relative humidity in percent (0 - 100)
     * @return The heat alert level
     */
    fun getHeatAlert(tempCelsius: Float, relativeHumidity: Float): HeatAlert {
        val heatIndex = celsiusToFahrenheit(getHeatIndex(tempCelsius, relativeHumidity))

        return when {
            heatIndex < 80 -> HeatAlert.NORMAL
            heatIndex <= 90 -> HeatAlert.CAUTION
            heatIndex <= 103 -> HeatAlert.EXTREME_CAUTION
            heatIndex <= 125 -> HeatAlert.DANGER
            else -> HeatAlert.EXTREME_DANGER
        }
    }

    enum class PrecipitationType {
        RAIN,
        SNOW,
        UNKNOWN
    }

    /**
     * Get the likely precipitation type at the given temperature
     * @param tempCelsius The termperature in celsius
     * @return The most likely precipitation type
     */
    fun getLikelyPrecipitationType(tempCelsius: Float): PrecipitationType {
        return when {
            tempCelsius <= -2 -> PrecipitationType.SNOW
            tempCelsius >= 2 -> PrecipitationType.RAIN
            else -> PrecipitationType.UNKNOWN
        }
    }

    /**
     * Get the dew point in celsius
     * @param tempCelsius The temperature in celsius
     * @param relativeHumidity The relative humidity in percent (0 - 100)
     * @return The dew point in celsius
     */
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

    enum class HumidityComfortLevel(val readableName: String) {
        PLEASANT("Pleasant"),
        COMFORTABLE("Comfortable"),
        STICKY("Sticky"),
        UNCOMFORTABLE("Uncomfortable"),
        OPPRESSIVE("Oppressive"),
        MISERABLE("Miserable")
    }

    /**
     * Convert celsius to fahrenheit
     * @param tempCelsius The temperature in celsius
     * @return The temperature in fahrenheit
     */
    fun celsiusToFahrenheit(tempCelsius: Float): Float {
        return tempCelsius * 9 / 5f + 32
    }

    /**
     * Convert fahrenheit to celsius
     * @param tempFahrenheit The temperature in fahrenheit
     * @return The temperature in celsius
     */
    fun fahrenheitToCelsius(tempFahrenheit: Float): Float {
        return (tempFahrenheit - 32) * 5 / 9f + 32
    }

    /**
     * Convert hPa to inches
     * @param pressure The pressure in hPa
     * @return The pressure in inches
     */
    fun hPaToInches(pressure: Float): Float {
        return 0.030f * pressure
    }

    /**
     * Get the comfort level at the given humidity
     * @param tempCelsius The temperature in celsius
     * @param relativeHumidity The relative humidity in percent (0 - 100)
     * @return The comfort level
     */
    fun getHumidityComfortLevel(tempCelsius: Float, relativeHumidity: Float): HumidityComfortLevel {
        val dewPoint = celsiusToFahrenheit(getDewPoint(tempCelsius, relativeHumidity))
        return when {
            dewPoint <= 55 -> HumidityComfortLevel.PLEASANT
            dewPoint <= 60 -> HumidityComfortLevel.COMFORTABLE
            dewPoint <= 65 -> HumidityComfortLevel.STICKY
            dewPoint <= 70 -> HumidityComfortLevel.UNCOMFORTABLE
            dewPoint <= 75 -> HumidityComfortLevel.OPPRESSIVE
            else -> HumidityComfortLevel.MISERABLE
        }
    }


    enum class BarometricChange {
        FALLING,
        RISING,
        NO_CHANGE
    }

    /**
     * Get the barometric pressure change direction
     * @param readings The barometric pressure readings in hPa
     * @return The direction of change
     */
    fun getBarometricChangeDirection(readings: Array<Float>): BarometricChange {
        val change = getBarometricChange(readings)

        return when {
            abs(change) < 1 -> BarometricChange.NO_CHANGE
            change < 0 -> BarometricChange.FALLING
            else -> BarometricChange.RISING
        }
    }

    /**
     * Get the barometric pressure change
     * @param readings The barometric pressure readings in hPa
     * @return The pressure change in hPa
     */
    fun getBarometricChange(readings: Array<Float>): Float {
        if (readings.size < 2) return 0f
        val firstReading = readings.first()
        val lastReading = readings.last()
        val change = lastReading - firstReading
        return change
    }

}