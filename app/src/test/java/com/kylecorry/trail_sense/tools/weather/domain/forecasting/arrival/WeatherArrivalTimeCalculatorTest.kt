package com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherForecast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal class WeatherArrivalTimeCalculatorTest {

    @Test
    fun usesTheForecastedTimeWhenItIsInTheFuture() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = now.plus(Duration.ofHours(2)))),
            emptyList()
        )

        assertEquals(now.plus(Duration.ofHours(2)), arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun roundsTheForecastedTimeToTheNearest15Minutes() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = now.plus(Duration.ofMinutes(38)))),
            emptyList()
        )

        // 12:38 rounds up to 12:45
        assertEquals(now.plus(Duration.ofMinutes(45)), arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun returnsTheForecastedTimeUnroundedWhenItIsInThePast() {
        val past = now.minus(Duration.ofMinutes(38))

        val arrival = calculator().getArrivalTime(listOf(forecast(time = past)), emptyList())

        assertEquals(past, arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun returnsNullWhenThereIsNoForecastedTimeAndNoConditions() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = null, conditions = emptyList())),
            emptyList()
        )

        assertNull(arrival)
    }

    @Test
    fun stormsArriveVerySoon() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Storm),
                    tendency = PressureTendency(PressureCharacteristic.Steady, 0f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(Duration.ofHours(1)), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun rapidTendencyArrivesVerySoon() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Rain),
                    tendency = PressureTendency(PressureCharacteristic.FallingFast, -1f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(Duration.ofHours(1)), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun slowlyChangingTendencyArrivesSoon() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Rain),
                    tendency = PressureTendency(PressureCharacteristic.Falling, -0.5f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(Duration.ofHours(2)), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun steadyTendencyArrivesLater() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Rain),
                    tendency = PressureTendency(PressureCharacteristic.Steady, 0f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(Duration.ofHours(8)), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun aMissingTendencyIsTreatedAsSteady() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = null, conditions = listOf(WeatherCondition.Rain))),
            emptyList()
        )

        assertEquals(now.plus(Duration.ofHours(8)), arrival?.time)
    }

    @Test
    fun onlyTheFirstForecastIsUsed() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(time = null, conditions = emptyList()),
                forecast(time = now.plus(Duration.ofHours(2)))
            ),
            emptyList()
        )

        assertNull(arrival)
    }

    private fun calculator(): WeatherArrivalTimeCalculator {
        return WeatherArrivalTimeCalculator(Clock.fixed(now, ZoneId.of("UTC")))
    }

    private fun forecast(
        time: Instant?,
        conditions: List<WeatherCondition> = listOf(WeatherCondition.Rain),
        tendency: PressureTendency? = null
    ): WeatherForecast {
        return WeatherForecast(time, conditions, tendency = tendency)
    }

    companion object {
        // 2024-01-01T12:00:00Z
        private val now = Instant.ofEpochSecond(1704110400)
    }
}
