package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.calculators

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

internal class DailyTemperatureCalculatorTest {

    @Test
    fun theCoolestPartOfTheDayIsAroundSunrise() = runBlocking {
        val sunrise = temperature(date.atTime(6, 30))
        val beforeSunrise = temperature(date.atTime(4, 0))
        val afterSunrise = temperature(date.atTime(9, 0))

        assertTrue(sunrise < beforeSunrise, "$sunrise should be below $beforeSunrise")
        assertTrue(sunrise < afterSunrise, "$sunrise should be below $afterSunrise")
        assertEquals(10f, sunrise, 1f)
    }

    @Test
    fun theWarmestPartOfTheDayIsAFewHoursAfterSolarNoon() = runBlocking {
        val afternoon = temperature(date.atTime(15, 30))
        val noon = temperature(date.atTime(12, 0))
        val evening = temperature(date.atTime(19, 0))

        assertTrue(afternoon > noon, "$afternoon should be above $noon")
        assertTrue(afternoon > evening, "$afternoon should be above $evening")
        assertEquals(20f, afternoon, 1f)
    }

    @Test
    fun temperaturesStayWithinTheDailyRange() = runBlocking {
        val calculator = calculator()

        for (hour in 0..23) {
            val temperature =
                calculator.calculate(zoned(date.atTime(hour, 0))).celsius().value
            assertTrue(
                temperature in 9f..21f,
                "Hour $hour was $temperature, outside of the 10 - 20 range"
            )
        }
    }

    @Test
    fun eachTimeOfDayHasAStableTemperature() = runBlocking {
        val calculator = calculator()
        val time = zoned(date.atTime(9, 0))

        val first = calculator.calculate(time)
        val second = calculator.calculate(time)

        assertEquals(first.celsius().value, second.celsius().value, 0.001f)
    }

    @Test
    fun theDailyRangeIsLookedUpAtMostOncePerDayPerSegment() = runBlocking {
        val calls = AtomicInteger()
        val calculator = calculator(calls)

        // All of these fall in the afternoon segment of the same day
        calculator.calculate(zoned(date.atTime(13, 0)))
        val afterFirst = calls.get()
        calculator.calculate(zoned(date.atTime(14, 0)))
        calculator.calculate(zoned(date.atTime(15, 0)))

        assertEquals(afterFirst, calls.get())
    }

    @Test
    fun midnightBelongsToThePreviousAfternoonsCurve() = runBlocking {
        val calculator = calculator()

        val beforeMidnight = calculator.calculate(zoned(date.atTime(23, 55))).celsius().value
        val afterMidnight =
            calculator.calculate(zoned(date.plusDays(1).atTime(0, 5))).celsius().value

        assertEquals(beforeMidnight, afterMidnight, 0.5f)
    }

    @Test
    fun theTemperatureIsContinuousAcrossTheDay() = runBlocking {
        val calculator = calculator()

        var previous = calculator.calculate(zoned(date.atTime(0, 0))).celsius().value
        for (minutes in 15..(24 * 60) step 15) {
            val time = zoned(date.atStartOfDay().plusMinutes(minutes.toLong()))
            val temperature = calculator.calculate(time).celsius().value
            assertTrue(
                kotlin.math.abs(temperature - previous) < 2f,
                "Jump of ${temperature - previous} at $time"
            )
            previous = temperature
        }
    }

    @Test
    fun usesTheRangeOfTheDayBeingCalculated() = runBlocking {
        val calculator = DailyTemperatureCalculator(location) { _, day ->
            if (day == date) {
                Range(Temperature.celsius(10f), Temperature.celsius(20f))
            } else {
                Range(Temperature.celsius(30f), Temperature.celsius(40f))
            }
        }

        val today = calculator.calculate(zoned(date.atTime(15, 30))).celsius().value
        val muchLater = calculator.calculate(zoned(date.plusDays(10).atTime(15, 30))).celsius().value

        assertEquals(20f, today, 1f)
        assertEquals(40f, muchLater, 1f)
    }

    @Test
    fun worksInTheSouthernHemisphere() = runBlocking {
        val southern = Coordinate(-33.9, 151.2)
        val zone = ZoneId.of("Australia/Sydney")
        val calculator = DailyTemperatureCalculator(southern) { _, _ ->
            Range(Temperature.celsius(10f), Temperature.celsius(20f))
        }

        for (hour in 0..23) {
            val temperature = calculator.calculate(
                ZonedDateTime.of(date.atTime(hour, 0), zone)
            ).celsius().value
            assertTrue(
                temperature in 9f..21f,
                "Hour $hour was $temperature, outside of the 10 - 20 range"
            )
        }
    }

    @Test
    fun worksAtHighLatitudesWhereTheSunMayNotRise() = runBlocking {
        val arctic = Coordinate(78.2, 15.6)
        val polarNight = LocalDate.of(2024, 12, 21)
        val calculator = DailyTemperatureCalculator(arctic) { _, _ ->
            Range(Temperature.celsius(-20f), Temperature.celsius(-10f))
        }

        for (hour in 0..23) {
            val temperature = calculator.calculate(
                ZonedDateTime.of(polarNight.atTime(hour, 0), ZoneId.of("UTC"))
            ).celsius().value
            assertTrue(
                temperature in -21f..-9f,
                "Hour $hour was $temperature, outside of the -20 - -10 range"
            )
        }
    }

    private suspend fun temperature(time: java.time.LocalDateTime): Float {
        return calculator().calculate(zoned(time)).celsius().value
    }

    private fun zoned(time: java.time.LocalDateTime): ZonedDateTime {
        return ZonedDateTime.of(time, zone)
    }

    private fun calculator(calls: AtomicInteger? = null): DailyTemperatureCalculator {
        return DailyTemperatureCalculator(location) { _, _ ->
            calls?.incrementAndGet()
            Range(Temperature.celsius(10f), Temperature.celsius(20f))
        }
    }

    companion object {
        private val location = Coordinate(40.0, -80.0)
        private val zone = ZoneId.of("America/New_York")
        private val date = LocalDate.of(2024, 6, 21)
    }
}
