package com.kylecorry.trail_sense.astronomy.domain.moon

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.Month

class MoonStateCalculatorTest {

    @Test
    fun worksWhenMoonSetsThenRises() {
        val moonTimes = MoonTimes(
            LocalDateTime.of(2020, Month.APRIL, 2, 12, 50),
            LocalDateTime.of(2020, Month.APRIL, 2, 3, 30))

        val calculator = MoonStateCalculator()

        val upTime1 = LocalTime.MIN
        val downTime = LocalTime.of(11, 0)
        val upTime2 = LocalTime.MAX

        assertTrue(calculator.isUp(moonTimes, upTime1))
        assertFalse(calculator.isUp(moonTimes, downTime))
        assertTrue(calculator.isUp(moonTimes, upTime2))
    }

    @Test
    fun worksWhenMoonRisesThenSets() {
        val moonTimes = MoonTimes(
            LocalDateTime.of(2020, Month.APRIL, 2, 3, 50),
            LocalDateTime.of(2020, Month.APRIL, 2, 12, 30))

        val calculator = MoonStateCalculator()

        val downTime1 = LocalTime.MIN
        val upTime = LocalTime.of(11, 0)
        val downTime2 = LocalTime.MAX

        assertFalse(calculator.isUp(moonTimes, downTime1))
        assertTrue(calculator.isUp(moonTimes, upTime))
        assertFalse(calculator.isUp(moonTimes, downTime2))
    }

    @Test
    fun worksWhenMoonOnlyRises() {
        val moonTimes = MoonTimes(
            LocalDateTime.of(2020, Month.APRIL, 2, 12, 0),
            null)

        val calculator = MoonStateCalculator()

        val downTime = LocalTime.MIN
        val upTime = LocalTime.MAX

        assertFalse(calculator.isUp(moonTimes, downTime))
        assertTrue(calculator.isUp(moonTimes, upTime))
    }

    @Test
    fun worksWhenMoonOnlySets() {
        val moonTimes = MoonTimes(
            null,
            LocalDateTime.of(2020, Month.APRIL, 2, 12, 0))

        val calculator = MoonStateCalculator()

        val upTime = LocalTime.MIN
        val downTime = LocalTime.MAX

        assertTrue(calculator.isUp(moonTimes, upTime))
        assertFalse(calculator.isUp(moonTimes, downTime))
    }

    @Test
    fun returnsFalseWhenMoonUnknown(){
        val moonTimes = MoonTimes(null, null)

        val calculator = MoonStateCalculator()

        val time = LocalTime.MIN

        assertFalse(calculator.isUp(moonTimes, time))
    }
}