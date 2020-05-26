package com.kylecorry.trail_sense.astronomy.domain

import org.threeten.bp.LocalDateTime

data class AstroAltitude(val time: LocalDateTime, val altitudeDegrees: Float){
    val altitudeRadians = Math.toRadians(altitudeDegrees.toDouble()).toFloat()
}