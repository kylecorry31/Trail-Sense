package com.kylecorry.trail_sense.astronomy.domain.sun

import org.threeten.bp.LocalDateTime

class SunTimes(val up: LocalDateTime?, val down: LocalDateTime?, val isAlwaysUp: Boolean = false, val isAlwaysDown: Boolean = false) {

    companion object {
        fun unknown(): SunTimes {
            return SunTimes(null, null)
        }

        fun alwaysUp(): SunTimes {
            return SunTimes(null, null, true)
        }

        fun alwaysDown(): SunTimes {
            return SunTimes(null, null, false, true)
        }

    }

}