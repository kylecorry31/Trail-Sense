package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.database.Identifiable
import kotlin.math.pow

data class RawWeatherObservation(
    override val id: Long,
    val pressure: Float,
    val altitude: Float,
    val temperature: Float,
    val altitudeError: Float? = null,
    val humidity: Float? = null
) : Identifiable {
    fun seaLevel(useTemperature: Boolean = true): Pressure {
        val pressure = if (useTemperature) {
            pressure * (1 - ((0.0065f * altitude) / (temperature + 0.0065f * altitude + 273.15f))).pow(
                -5.257f
            )
        } else {
            pressure * (1 - altitude / 44330.0).pow(-5.255).toFloat()
        }
        return Pressure.hpa(pressure)
    }
}