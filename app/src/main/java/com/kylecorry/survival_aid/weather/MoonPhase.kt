package com.kylecorry.survival_aid.weather

import java.time.LocalDate

class MoonPhase {

    enum class Phase {
        NEW,
        WAXING_CRESCENT,
        FIRST_QUARTER,
        WAXING_GIBBOUS,
        FULL,
        WANING_GIBBOUS,
        LAST_QUARTER,
        WANING_CRESCENT
    }

    /**
     * Get the current phase of the moon
     * @return The moon phase
     */
    fun getPhase(time: LocalDate = LocalDate.now()): Phase {
        var month = time.monthValue
        var year = time.year
        val day = time.dayOfMonth

        if (month == 1){
            month = 13
            year--
        } else if (month == 2){
            month = 14
            year--
        }


        val result = (year % 19) * 11 + (month - 3) + day
        val daysSinceNewMoon = result % 30

        return when {
            daysSinceNewMoon == 0 || daysSinceNewMoon == 29 -> Phase.NEW
            daysSinceNewMoon < 8 -> Phase.WAXING_CRESCENT
            daysSinceNewMoon == 8 -> Phase.FIRST_QUARTER
            daysSinceNewMoon < 15 -> Phase.WAXING_GIBBOUS
            daysSinceNewMoon == 15 -> Phase.FULL
            daysSinceNewMoon < 22 -> Phase.WANING_GIBBOUS
            daysSinceNewMoon == 22 -> Phase.LAST_QUARTER
            daysSinceNewMoon <= 29 -> Phase.WANING_CRESCENT
            else -> Phase.NEW
        }
    }
}