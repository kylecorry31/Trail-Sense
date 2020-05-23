package com.kylecorry.trail_sense.astronomy.domain.moon

import org.threeten.bp.LocalTime

class MoonStateCalculator {

    fun isUp(moonTimes: MoonTimes, time: LocalTime = LocalTime.now()): Boolean {

        if (moonTimes.up == null && moonTimes.down == null){
            return false
        }

        val dateTime = (moonTimes.up ?: moonTimes.down)?.toLocalDate()?.atTime(time)!!

        if (moonTimes.up == null){
            return moonTimes.down?.isAfter(dateTime) == true
        }

        if (moonTimes.down == null){
            return moonTimes.up.isBefore(dateTime)
        }

        if (moonTimes.up.isBefore(moonTimes.down)){
            return when {
                moonTimes.up.isAfter(dateTime) -> {
                    false
                }
                moonTimes.down.isAfter(dateTime) -> {
                    true
                }
                else -> {
                    false
                }
            }
        } else {
            return when {
                moonTimes.down.isAfter(dateTime) -> {
                    true
                }
                moonTimes.up.isAfter(dateTime) -> {
                    false
                }
                else -> {
                    true
                }
            }
        }

    }


}