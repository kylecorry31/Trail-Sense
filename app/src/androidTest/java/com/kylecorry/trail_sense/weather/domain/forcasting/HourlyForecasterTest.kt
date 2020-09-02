package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.trail_sense.weather.domain.PressureReading
import org.junit.Assert
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HourlyForecasterTest {

    @Test
    fun forecastWorseningSlow() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3).minusMinutes(5)
                ), 1017f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1016.5f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1016f
            ),
            PressureReading(
                Instant.now(),
                1016.1f
            )
        )

        val forecaster = HourlyForecaster(-6f, 0.5f)

        val prediction = forecaster.forecast(pressures)

        Assert.assertEquals(Weather.WorseningSlow, prediction)
    }

    @Test
    fun forecastImprovingSlow() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3).minusMinutes(5)
                ), 1007f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1007.5f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1008f
            ),
            PressureReading(
                Instant.now(),
                1008.2f
            )
        )

        val forecaster = HourlyForecaster(-6f, 0.5f)

        val prediction = forecaster.forecast(pressures)

        Assert.assertEquals(Weather.ImprovingSlow, prediction)
    }

    @Test
    fun forecastWorseningFast() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3).minusMinutes(5)
                ), 1017f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1016f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1015f
            ),
            PressureReading(
                Instant.now(),
                1014.21f
            )
        )

        val forecaster = HourlyForecaster(-6f, 0.5f)

        val prediction = forecaster.forecast(pressures)

        Assert.assertEquals(Weather.WorseningFast, prediction)
    }

    @Test
    fun forecastStorm() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3).minusMinutes(5)
                ), 1017f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1015f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1013f
            ),
            PressureReading(
                Instant.now(),
                1010f
            )
        )

        val forecaster = HourlyForecaster(-6f, 2f)

        val prediction = forecaster.forecast(pressures)

        Assert.assertEquals(Weather.Storm, prediction)
    }

    @Test
    fun forecastImprovingFast() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3).minusMinutes(5)
                ), 1007f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(2)
                ), 1008f
            ),
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(1)
                ), 1008.3f
            ),
            PressureReading(
                Instant.now(),
                1009.8f
            )
        )

        val forecaster = HourlyForecaster(-6f, 0.5f)

        val prediction = forecaster.forecast(pressures)

        Assert.assertEquals(Weather.ImprovingFast, prediction)
    }

    @Test
    fun forecastSteady() {
        val pressures = listOf(
            PressureReading(
                Instant.now().minus(
                    Duration.ofHours(3).minusMinutes(5)
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
                1007.4f
            )
        )

        val forecaster = HourlyForecaster(-6f, 2f)

        val prediction = forecaster.forecast(pressures)

        Assert.assertEquals(Weather.NoChange, prediction)
    }
}