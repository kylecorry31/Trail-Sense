package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
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

fun CoordinateBounds.grow(percent: Float): CoordinateBounds {
    val x = this.width() * percent
    val y = this.height() * percent
    return CoordinateBounds.Companion.from(
        listOf(
            northWest.plus(x, Bearing.Companion.from(CompassDirection.West))
                .plus(y, Bearing.Companion.from(CompassDirection.North)),
            northEast.plus(x, Bearing.Companion.from(CompassDirection.East))
                .plus(y, Bearing.Companion.from(CompassDirection.North)),
            southWest.plus(x, Bearing.Companion.from(CompassDirection.West))
                .plus(y, Bearing.Companion.from(CompassDirection.South)),
            southEast.plus(x, Bearing.Companion.from(CompassDirection.East))
                .plus(y, Bearing.Companion.from(CompassDirection.South)),
        )
    )
}

fun CoordinateBounds.intersects2(other: CoordinateBounds): Boolean {
    if (intersects(other)){
        return true
    }

    if (south > other.north || other.south > north) {
        return false
    }

    val selfWraps = east < west
    val otherWraps = other.east < other.west

    if (selfWraps && otherWraps) return true
    if (selfWraps) return other.west <= east || other.east >= west
    if (otherWraps) return west <= other.east || east >= other.west
    return west <= other.east && east >= other.west
}
