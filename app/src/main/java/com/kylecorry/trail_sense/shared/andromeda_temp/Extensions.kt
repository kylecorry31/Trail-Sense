package com.kylecorry.trail_sense.shared.andromeda_temp

import kotlin.math.log
import kotlin.math.sqrt
import kotlin.random.Random

fun Random.nextGaussian(): Double {
    // From Random.java
    var v1: Double
    var v2: Double
    var s: Double
    do {
        v1 = nextDouble(-1.0, 1.0)
        v2 = nextDouble(-1.0, 1.0)
        s = v1 * v1 + v2 * v2
    } while (s >= 1 || s == 0.0)
    val multiplier = sqrt(-2 * log(s, 10.0) / s)
    return v1 * multiplier
}