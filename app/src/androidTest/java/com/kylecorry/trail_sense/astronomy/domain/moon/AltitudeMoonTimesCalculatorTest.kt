package com.kylecorry.trail_sense.astronomy.domain.moon

import com.kylecorry.trail_sense.shared.Coordinate
import org.junit.Test

import org.junit.Assert.*
import java.time.*

class AltitudeMoonTimesCalculatorTest {

    private val location = Coordinate(41.817,-71.491)
    private val threshold = Duration.ofMinutes(20)

    @Test
    fun calculatesDaysWithBothRiseAndSet() {
        val calculator = AltitudeMoonTimesCalculator()

        val setThenRise = calculator.calculate(location, LocalDate.of(2020, Month.APRIL, 1))
        val riseThenSet = calculator.calculate(location, LocalDate.of(2020, Month.APRIL, 13))

        val expectedSetThenRise = MoonTimes(LocalDateTime.of(2020, Month.APRIL, 1, 11, 38), LocalDateTime.of(2020, Month.APRIL, 1, 2, 20))
        val expectedRiseThenSet = MoonTimes(LocalDateTime.of(2020, Month.APRIL, 13, 1, 2), LocalDateTime.of(2020, Month.APRIL, 13, 10, 17))


        assertMoonTimes(expectedSetThenRise, setThenRise)
        assertMoonTimes(expectedRiseThenSet, riseThenSet)
    }

    @Test
    fun calculatesDayWithOnlyRise() {
        val calculator = AltitudeMoonTimesCalculator()

        val actual = calculator.calculate(location, LocalDate.of(2020, Month.APRIL, 27))

        val expected = MoonTimes(LocalDateTime.of(2020, Month.APRIL, 27, 8, 41), null)

        assertMoonTimes(expected, actual)
    }

    @Test
    fun calculatesDayWithOnlySet() {
        val calculator = AltitudeMoonTimesCalculator()

        val actual = calculator.calculate(location, LocalDate.of(2020, Month.JANUARY, 17))

        val expected = MoonTimes(null, LocalDateTime.of(2020, Month.JANUARY, 17, 11, 29))

        assertMoonTimes(expected, actual)
    }


    private fun assertMoonTimes(expected: MoonTimes, actual: MoonTimes){
        if (expected.up == null || actual.up == null){
            assertEquals(expected.up, actual.up)
        } else {
            assertTrue("Expected ${expected.up} was ${actual.up}", Duration.between(expected.up, actual.up).abs() <= threshold)
        }

        if (expected.down == null || actual.down == null){
            assertEquals(expected.down, actual.down)
        } else {
            assertTrue("Expected ${expected.down} was ${actual.down}", Duration.between(expected.down, actual.down).abs() <= threshold)
        }
    }

}