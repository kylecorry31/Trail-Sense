package com.kylecorry.trail_sense.shared

fun constrain(value: Float, minimum: Float, maximum: Float): Float {
    return value.coerceIn(minimum, maximum)
}

fun lerp(start: Float, end: Float, percent: Float): Float {
    return start + (end - start) * percent
}

fun map(value: Float, originalMin: Float, originalMax: Float, newMin: Float, newMax: Float): Float {
    val normal = norm(value, originalMin, originalMax)
    return lerp(newMin, newMax, normal)
}

fun norm(value: Float, minimum: Float, maximum: Float): Float {
    val range = maximum - minimum
    if (range == 0f){
        return 0f
    }
    return (value - minimum) / range
}