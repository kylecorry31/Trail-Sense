package com.kylecorry.survival_aid.weather

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class MoonPhaseTest {
    @Test
    fun test(){
        val moonPhase = MoonPhase()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.YEAR, 2019)

        // New moon
        calendar.set(Calendar.DAY_OF_MONTH, 26)
        assertEquals(MoonPhase.Phase.NEW, moonPhase.getPhase(calendar))

        // Waxing crescent
        calendar.set(Calendar.DAY_OF_MONTH, 27)
        assertEquals(MoonPhase.Phase.WAXING_CRESCENT, moonPhase.getPhase(calendar))

        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.YEAR, 2020)
        calendar.set(Calendar.DAY_OF_MONTH, 2)
        assertEquals(MoonPhase.Phase.WAXING_CRESCENT, moonPhase.getPhase(calendar))

        // First quarter
        calendar.set(Calendar.DAY_OF_MONTH, 3)
        assertEquals(MoonPhase.Phase.FIRST_QUARTER, moonPhase.getPhase(calendar))

        // Waxing gibbous
        calendar.set(Calendar.DAY_OF_MONTH, 4)
        assertEquals(MoonPhase.Phase.WAXING_GIBBOUS, moonPhase.getPhase(calendar))

        calendar.set(Calendar.DAY_OF_MONTH, 9)
        assertEquals(MoonPhase.Phase.WAXING_GIBBOUS, moonPhase.getPhase(calendar))

        // Full moon
        calendar.set(Calendar.DAY_OF_MONTH, 10)
        assertEquals(MoonPhase.Phase.FULL, moonPhase.getPhase(calendar))

        // Waning gibbous
        calendar.set(Calendar.DAY_OF_MONTH, 11)
        assertEquals(MoonPhase.Phase.WANING_GIBBOUS, moonPhase.getPhase(calendar))

        calendar.set(Calendar.DAY_OF_MONTH, 16)
        assertEquals(MoonPhase.Phase.WANING_GIBBOUS, moonPhase.getPhase(calendar))

        // Last quarter
        calendar.set(Calendar.DAY_OF_MONTH, 17)
        assertEquals(MoonPhase.Phase.LAST_QUARTER, moonPhase.getPhase(calendar))

        // Waning crescent
        calendar.set(Calendar.DAY_OF_MONTH, 18)
        assertEquals(MoonPhase.Phase.WANING_CRESCENT, moonPhase.getPhase(calendar))

        calendar.set(Calendar.DAY_OF_MONTH, 23)
        assertEquals(MoonPhase.Phase.WANING_CRESCENT, moonPhase.getPhase(calendar))

        // New moon
        calendar.set(Calendar.DAY_OF_MONTH, 24)
        assertEquals(MoonPhase.Phase.NEW, moonPhase.getPhase(calendar))

    }
}