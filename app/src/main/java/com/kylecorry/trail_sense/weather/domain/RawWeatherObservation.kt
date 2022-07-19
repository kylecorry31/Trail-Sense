package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.WeatherService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.database.Identifiable

data class RawWeatherObservation(
    override val id: Long,
    val pressure: Float,
    val altitude: Float,
    val temperature: Float,
    val altitudeError: Float? = null,
    val humidity: Float? = null
) : Identifiable {
    fun seaLevel(useTemperature: Boolean = true): Pressure {
        val service = WeatherService()
        return service.getSeaLevelPressure(
            Pressure.hpa(pressure),
            Distance.meters(altitude),
            if (useTemperature) Temperature.celsius(temperature) else null
        )
    }
}