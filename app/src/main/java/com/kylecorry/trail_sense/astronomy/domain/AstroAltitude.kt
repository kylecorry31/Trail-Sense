package com.kylecorry.trail_sense.astronomy.domain

import java.time.LocalDateTime

data class AstroAltitude(val time: LocalDateTime, val altitudeDegrees: Float){
    val altitudeRadians = Math.toRadians(altitudeDegrees.toDouble()).toFloat()
}