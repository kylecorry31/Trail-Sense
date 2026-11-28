package com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival

import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal class WeatherArrivalTimeCalculatorTest {

    @Test
    fun usesTheForecastedTimeWhenItIsInTheFuture() {
        val arrival = calculator().getArrivalTime(now.plus(Duration.ofHours(2)), RelativeArrivalTime.Later)

        assertEquals(now.plus(Duration.ofHours(2)), arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun roundsTheForecastedTimeToTheNearest15Minutes() {
        val arrival = calculator().getArrivalTime(now.plus(Duration.ofMinutes(38)), null)

        // 12:38 rounds up to 12:45
        assertEquals(now.plus(Duration.ofMinutes(45)), arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun returnsTheForecastedTimeUnroundedWhenItIsInThePast() {
        val past = now.minus(Duration.ofMinutes(38))

        val arrival = calculator().getArrivalTime(past, null)

        assertEquals(past, arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @ParameterizedTest
    @CsvSource("60, Soon", "180, Soon", "240, Later")
    fun anExactArrivalIsSoonWithinThreeHours(minutes: Long, expected: RelativeArrivalTime) {
        val arrival = calculator().getArrivalTime(now.plus(Duration.ofMinutes(minutes)), null)

        assertEquals(expected, arrival?.relative)
    }

    @Test
    fun returnsNullWhenThereIsNoForecastedTimeOrArrival() {
        assertNull(calculator().getArrivalTime(null, null))
    }

    @ParameterizedTest
    @EnumSource(value = RelativeArrivalTime::class, names = ["Soon", "Later"])
    fun usesTheConditionsArrivalWithoutAForecastedTime(expected: RelativeArrivalTime) {
        val arrival = calculator().getArrivalTime(null, expected)

        assertEquals(expected, arrival?.relative)
        assertFalse(arrival!!.isExact)
    }

    private fun calculator(): WeatherArrivalTimeCalculator {
        return WeatherArrivalTimeCalculator(Clock.fixed(now, ZoneId.of("UTC")))
    }

    companion object {
        // 2024-01-01T12:00:00Z
        private val now = Instant.ofEpochSecond(1704110400)
    }
}
