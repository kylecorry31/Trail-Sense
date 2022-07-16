package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.Identifiable

data class RawWeatherObservation(
    override val id: Long,
    val pressure: Float,
    val altitude: Float,
    val temperature: Float,
    val altitudeError: Float? = null,
    val humidity: Float? = null
) : Identifiable

fun Reading<RawWeatherObservation>.toPressureAltitudeReading(): PressureAltitudeReading {
    return PressureAltitudeReading(
        time,
        value.pressure,
        value.altitude,
        value.temperature,
        value.altitudeError,
        value.humidity
    )
}