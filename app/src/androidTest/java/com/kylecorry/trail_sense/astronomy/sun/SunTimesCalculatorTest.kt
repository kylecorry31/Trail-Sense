package com.kylecorry.trail_sense.astronomy.sun

import com.kylecorry.trail_sense.shared.Coordinate
import org.junit.Test

import org.junit.Assert.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class SunTimesCalculatorTest {

    private val date1 = LocalDate.of(2020, 3, 29)
    private val date2 = LocalDate.of(2020, 1, 1)
    private val coordinates = Coordinate(41.91, -71.67)
    private val tolerance = Duration.ofMinutes(2).plusSeconds(30)

    @Test
    fun calculateActual() {
        val calculator = ActualTwilightCalculator()
        val date1Expected = SunTimes(date1.atTime(6, 34), date1.atTime(19, 9))
        val date2Expected = SunTimes(date2.atTime(7, 14), date2.atTime(16, 26))

        val date1Actual = calculator.calculate(coordinates, date1)
        val date2Actual = calculator.calculate(coordinates, date2)

        assertSunTimes(date1Expected, date1Actual, tolerance)
        assertSunTimes(date2Expected, date2Actual, tolerance)
    }

    @Test
    fun calculateCivil() {
        val calculator = CivilTwilightCalculator()
        val date1Expected = SunTimes(date1.atTime(6, 6), date1.atTime(19, 38))
        val date2Expected = SunTimes(date2.atTime(6, 42), date2.atTime(16, 57))

        val date1Actual = calculator.calculate(coordinates, date1)
        val date2Actual = calculator.calculate(coordinates, date2)

        assertSunTimes(date1Expected, date1Actual, tolerance)
        assertSunTimes(date2Expected, date2Actual, tolerance)
    }

    @Test
    fun calculateNautical() {
        val calculator = NauticalTwilightCalculator()
        val date1Expected = SunTimes(date1.atTime(5, 33), date1.atTime(20, 11))
        val date2Expected = SunTimes(date2.atTime(6, 7), date2.atTime(17, 32))

        val date1Actual = calculator.calculate(coordinates, date1)
        val date2Actual = calculator.calculate(coordinates, date2)

        assertSunTimes(date1Expected, date1Actual, tolerance)
        assertSunTimes(date2Expected, date2Actual, tolerance)
    }

    @Test
    fun calculateAstronomical() {
        val calculator = AstronomicalTwilightCalculator()
        val date1Expected = SunTimes(date1.atTime(4, 59), date1.atTime(20, 45))
        val date2Expected = SunTimes(date2.atTime(5, 34), date2.atTime(18, 6))

        val date1Actual = calculator.calculate(coordinates, date1)
        val date2Actual = calculator.calculate(coordinates, date2)

        assertSunTimes(date1Expected, date1Actual, tolerance)
        assertSunTimes(date2Expected, date2Actual, tolerance)
    }

    @Test
    fun calculatesNoon(){
        val up = LocalDateTime.of(2020, 3, 29, 6, 0)
        val down = LocalDateTime.of(2020, 3, 29, 20, 0)

        val sunTimes = SunTimes(up, down)

        val expected = LocalDateTime.of(2020, 3, 29, 13, 0)

        assertEquals(expected, sunTimes.noon)
    }


    private fun assertSunTimes(expected: SunTimes, actual: SunTimes, tolerance: Duration){

        val deltaUp = Duration.between(expected.up, actual.up).abs()
        val deltaDown = Duration.between(expected.down, actual.down).abs()

        assertTrue("Up time (${actual.up}) did not match expected (${expected.up})", deltaUp <= tolerance)
        assertTrue("Down time (${actual.down}) did not match expected (${expected.down})", deltaDown <= tolerance)
    }

}