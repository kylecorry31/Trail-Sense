package com.kylecorry.trail_sense.shared.andromeda_temp

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.subscriptions.ISubscription
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
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
    if (intersects(other)) {
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

fun Fragment.observe(
    subscription: ISubscription,
    state: BackgroundMinimumState = BackgroundMinimumState.Any,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    listener: suspend () -> Unit
) {
    observeFlow(subscription.flow(), state, collectOn, observeOn) { listener() }
}

fun <T> Fragment.observe(
    subscription: com.kylecorry.andromeda.core.subscriptions.generic.ISubscription<T>,
    state: BackgroundMinimumState = BackgroundMinimumState.Any,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    listener: suspend (T) -> Unit
) {
    observeFlow(subscription.flow(), state, collectOn, observeOn, listener)
}