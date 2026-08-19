package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class MonteCarloPressureForecasterTest {

    private val forecaster = MonteCarloPressureForecaster()
    private val start = Instant.ofEpochMilli(1700000000000)

    private fun history(vararg hpa: Float, stepHours: Long = 1): List<Reading<Pressure>> {
        return hpa.mapIndexed { i, value ->
            Reading(Pressure.hpa(value), start.plus(Duration.ofHours(i * stepHours)))
        }
    }

    @Test
    fun returnsNothingForEmptyHistory() {
        assertEquals(emptyList<Reading<Pressure>>(), forecaster.getPressureForecast(emptyList()))
    }

    @Test
    fun returnsNothingForTooFewReadings() {
        assertEquals(emptyList<Reading<Pressure>>(), forecaster.getPressureForecast(history(1000f)))
        assertEquals(
            emptyList<Reading<Pressure>>(),
            forecaster.getPressureForecast(history(1000f, 1001f))
        )
    }

    @Test
    fun forecastsAtTheRequestedStepAfterTheStartOfTheHistory() {
        val readings = history(1000f, 1001f, 1002f, 1003f, 1004f)

        val forecast = forecaster.getPressureForecast(readings, forecastLengthHours = 4f)

        assertTrue(forecast.isNotEmpty())
        assertTrue(forecast.size <= 4, "Expected at most 4 readings, got ${forecast.size}")
        val hoursFromStart = forecast.map {
            Duration.between(start, it.time).toMillis() / 3600000f
        }
        // The forecast starts after the last observation and steps by an hour
        assertTrue(hoursFromStart.all { it >= 4f }, "Forecast starts before the last reading")
        hoursFromStart.zipWithNext { a, b -> assertEquals(1f, b - a, 0.01f) }
    }

    @Test
    fun continuesARisingTrend() {
        val readings = history(1000f, 1001f, 1002f, 1003f, 1004f)

        val forecast = forecaster.getPressureForecast(readings, forecastLengthHours = 3f)

        val values = forecast.map { it.value.hpa().value }
        assertTrue(values.isNotEmpty())
        // The forecast is anchored on the last observation
        assertEquals(1004f, values.first(), 0.1f)
        values.zipWithNext { a, b -> assertTrue(b > a, "Expected the rise to continue: $values") }
    }

    @Test
    fun continuesAFallingTrend() {
        val readings = history(1004f, 1003f, 1002f, 1001f, 1000f)

        val forecast = forecaster.getPressureForecast(readings, forecastLengthHours = 3f)

        val values = forecast.map { it.value.hpa().value }
        assertTrue(values.isNotEmpty())
        assertEquals(1000f, values.first(), 0.1f)
        values.zipWithNext { a, b -> assertTrue(b < a, "Expected the fall to continue: $values") }
    }

    @Test
    fun holdsSteadyPressureSteady() {
        val readings = history(1013f, 1013f, 1013f, 1013f, 1013f)

        val forecast = forecaster.getPressureForecast(readings, forecastLengthHours = 3f)

        assertTrue(forecast.isNotEmpty())
        forecast.forEach {
            assertEquals(1013f, it.value.hpa().value, 1f)
        }
    }

    @Test
    fun isDeterministic() {
        val readings = history(1000f, 1002f, 1003f, 1002f, 1005f, 1004f)

        val first = forecaster.getPressureForecast(readings)
        val second = MonteCarloPressureForecaster().getPressureForecast(readings)

        assertEquals(first, second)
    }

    @Test
    fun dropsForecastsThatExceedTheErrorThreshold() {
        val readings = history(1000f, 1001f, 1002f, 1003f, 1004f)

        val forecast = forecaster.getPressureForecast(readings, maxErrorHpa = 0f)

        assertEquals(emptyList<Reading<Pressure>>(), forecast)
    }

    @Test
    fun aLongerForecastHasNoMoreReadingsThanRequested() {
        val readings = history(1000f, 1001f, 1002f, 1003f, 1004f)

        val short = forecaster.getPressureForecast(readings, forecastLengthHours = 2f)
        val long = forecaster.getPressureForecast(readings, forecastLengthHours = 12f)

        assertTrue(short.size <= 2)
        assertTrue(long.size <= 12)
        assertTrue(long.size >= short.size)
        // The overlapping portion of the forecast is the same
        assertEquals(short, long.take(short.size))
    }

    @Test
    fun aLargerVelocityErrorWidensTheForecastAndDropsMoreReadings() {
        val readings = history(1000f, 1001f, 1002f, 1003f, 1004f)

        val confident = forecaster.getPressureForecast(readings, velocityError = 0.01f)
        val uncertain = forecaster.getPressureForecast(readings, velocityError = 5f)

        assertTrue(
            uncertain.size < confident.size,
            "Expected fewer confident readings with a larger velocity error"
        )
    }

    @Test
    fun usesTheHistoryStepWhenReadingsAreNotHourly() {
        val readings = history(1000f, 1001f, 1002f, 1003f, 1004f, stepHours = 2)

        val forecast = forecaster.getPressureForecast(
            readings,
            forecastLengthHours = 4f,
            forecastStepSizeHours = 2f
        )

        assertTrue(forecast.isNotEmpty())
        val hoursFromStart = forecast.map {
            Duration.between(start, it.time).toMillis() / 3600000f
        }
        hoursFromStart.zipWithNext { a, b -> assertEquals(2f, b - a, 0.01f) }
    }
}
