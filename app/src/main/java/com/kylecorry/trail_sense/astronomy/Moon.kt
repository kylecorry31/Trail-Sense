package com.kylecorry.trail_sense.astronomy

import java.time.LocalDate

object Moon {

    enum class Phase(val longName: String) {
        New("New Moon"),
        WaxingCrescent("Waxing Crescent"),
        FirstQuarter("First Quarter"),
        WaxingGibbous("Waxing Gibbous"),
        Full("Full Moon"),
        WaningGibbous("Waning Gibbous"),
        LastQuarter("Last Quarter"),
        WaningCrescent("Waning Crescent")
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
            daysSinceNewMoon == 0 || daysSinceNewMoon == 29 -> Phase.New
            daysSinceNewMoon < 8 -> Phase.WaxingCrescent
            daysSinceNewMoon == 8 -> Phase.FirstQuarter
            daysSinceNewMoon < 15 -> Phase.WaxingGibbous
            daysSinceNewMoon == 15 -> Phase.Full
            daysSinceNewMoon < 22 -> Phase.WaningGibbous
            daysSinceNewMoon == 22 -> Phase.LastQuarter
            daysSinceNewMoon <= 29 -> Phase.WaningCrescent
            else -> Phase.New
        }
    }
}