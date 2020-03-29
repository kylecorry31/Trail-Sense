package com.kylecorry.trail_sense.shared

class Bearing(_value: Float){
    val value: Float = normalizeAngle(_value)
}