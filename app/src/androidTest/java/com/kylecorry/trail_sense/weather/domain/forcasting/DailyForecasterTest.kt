package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.sol.science.meteorology.forecast.Weather
import com.kylecorry.trail_sense.weather.domain.PressureReading
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DailyForecasterTest {

    @Test
    fun forecastWorsening() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3)
                ), 1017f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1013f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1010f
            ),
            PressureReading(
                Instant.now(),
                1007f
            )
        )

        val forecaster = DailyForecaster(0.5f)

        val prediction = forecaster.forecast(pressures)

        assertEquals(Weather.WorseningSlow, prediction)
    }

    @Test
    fun forecastImproving() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3)
                ), 1007f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1013f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1015f
            ),
            PressureReading(
                Instant.now(),
                1017f
            )
        )

        val forecaster = DailyForecaster(0.5f)

        val prediction = forecaster.forecast(pressures)

        assertEquals(Weather.ImprovingSlow, prediction)
    }

    @Test
    fun forecastSteady() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3)
                ), 1007f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1007.1f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1006.8f
            ),
            PressureReading(
                Instant.now(),
                1007.5f
            )
        )

        val forecaster = DailyForecaster(0.5f)

        val prediction = forecaster.forecast(pressures)

        assertEquals(Weather.Unknown, prediction)
    }
}