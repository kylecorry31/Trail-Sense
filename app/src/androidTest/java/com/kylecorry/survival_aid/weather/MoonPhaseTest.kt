package com.kylecorry.survival_aid.weather

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class MoonPhaseTest {
    @Test
    fun test(){
        val moonPhase = MoonPhase()
        var date = LocalDate.of(2019, Month.DECEMBER, 26)

        // New moon
        assertEquals(MoonPhase.Phase.NEW, moonPhase.getPhase(date))

        // Waxing crescent
        date = LocalDate.of(2019, Month.DECEMBER, 27)
        assertEquals(MoonPhase.Phase.WAXING_CRESCENT, moonPhase.getPhase(date))

        date = LocalDate.of(2020, Month.JANUARY, 2)
        assertEquals(MoonPhase.Phase.WAXING_CRESCENT, moonPhase.getPhase(date))

        // First quarter
        date = LocalDate.of(2020, Month.JANUARY, 3)
        assertEquals(MoonPhase.Phase.FIRST_QUARTER, moonPhase.getPhase(date))

        // Waxing gibbous
        date = LocalDate.of(2020, Month.JANUARY, 4)
        assertEquals(MoonPhase.Phase.WAXING_GIBBOUS, moonPhase.getPhase(date))

        date = LocalDate.of(2020, Month.JANUARY, 9)
        assertEquals(MoonPhase.Phase.WAXING_GIBBOUS, moonPhase.getPhase(date))

        // Full moon
        date = LocalDate.of(2020, Month.JANUARY, 10)
        assertEquals(MoonPhase.Phase.FULL, moonPhase.getPhase(date))

        // Waning gibbous
        date = LocalDate.of(2020, Month.JANUARY, 11)
        assertEquals(MoonPhase.Phase.WANING_GIBBOUS, moonPhase.getPhase(date))

        date = LocalDate.of(2020, Month.JANUARY, 16)
        assertEquals(MoonPhase.Phase.WANING_GIBBOUS, moonPhase.getPhase(date))

        // Last quarter
        date = LocalDate.of(2020, Month.JANUARY, 17)
        assertEquals(MoonPhase.Phase.LAST_QUARTER, moonPhase.getPhase(date))

        // Waning crescent
        date = LocalDate.of(2020, Month.JANUARY, 18)
        assertEquals(MoonPhase.Phase.WANING_CRESCENT, moonPhase.getPhase(date))

        date = LocalDate.of(2020, Month.JANUARY, 23)
        assertEquals(MoonPhase.Phase.WANING_CRESCENT, moonPhase.getPhase(date))

        // New moon
        date = LocalDate.of(2020, Month.JANUARY, 24)
        assertEquals(MoonPhase.Phase.NEW, moonPhase.getPhase(date))

    }
}