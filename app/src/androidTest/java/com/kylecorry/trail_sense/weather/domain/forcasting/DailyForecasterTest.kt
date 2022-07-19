package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DailyForecasterTest {

    @Test
    fun forecastWorsening() {
        val pressures = listOf(
            Reading(
                Pressure.hpa(1017f),
                Instant.now().minus(Duration.ofHours(3))
            ),
            Reading(
                Pressure.hpa(1013f),
                Instant.now().minus(Duration.ofHours(2))
            ),
            Reading(
                Pressure.hpa(1010f),
                Instant.now().minus(Duration.ofHours(1))
            ),
            Reading(
                Pressure.hpa(1007f),
                Instant.now()
            )
        )

        val forecaster = DailyForecaster(0.5f)

        val prediction = forecaster.forecast(pressures)

        assertEquals(Weather.WorseningSlow, prediction)
    }

    @Test
    fun forecastImproving() {
        val pressures = listOf(
            Reading(
                Pressure.hpa(1007f),
                Instant.now().minus(Duration.ofHours(3))
            ),
            Reading(
                Pressure.hpa(1013f),
                Instant.now().minus(Duration.ofHours(2))
            ),
            Reading(
                Pressure.hpa(1015f),
                Instant.now().minus(Duration.ofHours(1))
            ),
            Reading(
                Pressure.hpa(1017f),
                Instant.now()
            )
        )

        val forecaster = DailyForecaster(0.5f)

        val prediction = forecaster.forecast(pressures)

        assertEquals(Weather.ImprovingSlow, prediction)
    }

    @Test
    fun forecastSteady() {
        val pressures = listOf(
            Reading(
                Pressure.hpa(1007f),
                Instant.now().minus(Duration.ofHours(3))
            ),
            Reading(
                Pressure.hpa(1007.1f),
                Instant.now().minus(Duration.ofHours(2))
            ),
            Reading(
                Pressure.hpa(1006.8f),
                Instant.now().minus(Duration.ofHours(1))
            ),
            Reading(
                Pressure.hpa(1007.5f),
                Instant.now()
            )
        )

        val forecaster = DailyForecaster(0.5f)

        val prediction = forecaster.forecast(pressures)

        assertEquals(Weather.Unknown, prediction)
    }
}