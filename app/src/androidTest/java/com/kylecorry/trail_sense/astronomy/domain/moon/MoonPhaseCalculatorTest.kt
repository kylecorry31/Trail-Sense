package com.kylecorry.trail_sense.astronomy.domain.moon

import org.junit.Test

import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

class MoonPhaseCalculatorTest {

    @Test
    fun getPhase() {
        val calculator = MoonPhaseCalculator()

        val tolerance = 0.5f

        // Main phases
        assertMoonPhases(MoonPhase(MoonTruePhase.FirstQuarter, 50f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 2, 14, 58))), tolerance)
        assertMoonPhases(MoonPhase(MoonTruePhase.Full, 100f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 9, 13, 48))), tolerance)
        assertMoonPhases(MoonPhase(MoonTruePhase.ThirdQuarter, 50f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 16, 5, 35))), tolerance)
        assertMoonPhases(MoonPhase(MoonTruePhase.New, 0f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 24, 5, 29))), tolerance)

        // Intermediate phases
        assertMoonPhases(MoonPhase(MoonTruePhase.WaxingCrescent, 23f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 29, 12, 0))), tolerance)
        assertMoonPhases(MoonPhase(MoonTruePhase.WaxingGibbous, 79f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 5, 12, 0))), tolerance)
        assertMoonPhases(MoonPhase(MoonTruePhase.WaningGibbous, 79f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 13, 12, 0))), tolerance)
        assertMoonPhases(MoonPhase(MoonTruePhase.WaningCrescent, 28f), calculator.getPhase(getDate(LocalDateTime.of(2020, Month.MARCH, 18, 12, 0))), tolerance)
    }

    private fun getDate(time: LocalDateTime): ZonedDateTime {
        return time.atZone(ZoneId.of("America/New_York"))
    }


    private fun assertMoonPhases(expected: MoonPhase, actual: MoonPhase, tolerance: Float){
        assertEquals(expected.phase, actual.phase)
        assertEquals(expected.illumination, actual.illumination, tolerance)
    }

}