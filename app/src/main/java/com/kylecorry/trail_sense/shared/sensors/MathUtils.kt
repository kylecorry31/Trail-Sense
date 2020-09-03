package com.kylecorry.trail_sense.shared.sensors

object MathUtils {
    fun wrap(value: Float, min: Float, max: Float): Float {
        var newValue = value

        while(newValue < min){
            newValue += min
        }

        while (newValue > max){
            newValue -= max
        }

        return newValue
    }
}