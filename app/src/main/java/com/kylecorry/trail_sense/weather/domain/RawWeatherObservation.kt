package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Coordinate
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
    val humidity: Float? = null,
    val location: Coordinate = Coordinate.zero
) : Identifiable {
    fun seaLevel(useTemperature: Boolean = true): Pressure {
        return Meteorology.getSeaLevelPressure(
            Pressure.hpa(pressure),
            Distance.meters(altitude),
            if (useTemperature) Temperature.celsius(temperature) else null
        )
    }

    /**
     * Returns the 95% confidence interval for the sea level pressure reading
     */
    fun seaLevelConfidenceInterval(useTemperature: Boolean = true): Pair<Pressure, Pressure> {
        val lower = Meteorology.getSeaLevelPressure(
            Pressure.hpa(pressure),
            Distance.meters(altitude - 1.96f * (altitudeError ?: 0f)),
            if (useTemperature) Temperature.celsius(temperature) else null
        )

        val upper = Meteorology.getSeaLevelPressure(
            Pressure.hpa(pressure),
            Distance.meters(altitude + 1.96f * (altitudeError ?: 0f)),
            if (useTemperature) Temperature.celsius(temperature) else null
        )

        return lower to upper
    }
}