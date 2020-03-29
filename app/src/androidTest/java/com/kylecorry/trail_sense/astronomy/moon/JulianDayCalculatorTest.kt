package com.kylecorry.trail_sense.astronomy.moon

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.Month

class JulianDayCalculatorTest {

    @Test
    fun calculatesJulianDay(){

        val tolerance = 0.000000001

        assertEquals(2451545.0, JulianDayCalculator.calculate(LocalDateTime.of(2000, Month.JANUARY, 1, 12, 0)), tolerance)
        assertEquals(2451179.5, JulianDayCalculator.calculate(LocalDateTime.of(1999, Month.JANUARY, 1, 0, 0)), tolerance)

    }

}