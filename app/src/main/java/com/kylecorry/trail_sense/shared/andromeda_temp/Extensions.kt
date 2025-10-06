package com.kylecorry.trail_sense.shared.andromeda_temp

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.subscriptions.ISubscription
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.sol.math.SolMath.isCloseTo
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue

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

fun CoordinateBounds.heightDegrees(): Double {
    return (north - south).absoluteValue
}

fun CoordinateBounds.widthDegrees(): Double {
    if (isCloseTo(west, CoordinateBounds.world.west, 0.0001) && isCloseTo(
            east,
            CoordinateBounds.world.east,
            0.0001
        )
    ) {
        return 360.0
    }

    return (if (east >= west) {
        east - west
    } else {
        (180 - west) + (east + 180)
    }).absoluteValue
}

fun CoordinateBounds.intersects2(other: CoordinateBounds): Boolean {
    if (south > other.north || other.south > north) {
        return false
    }

    val union = CoordinateBounds.from(
        listOf(
            northEast, northWest, southEast, southWest,
            other.northEast, other.northWest, other.southEast, other.southWest
        )
    )

    return union.widthDegrees() <= (widthDegrees() + other.widthDegrees())
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

fun <T> ReactiveComponent.useBackgroundMemo2(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    cancelWhenRerun: Boolean = false,
    block: suspend CoroutineScope.() -> T
): T? {
    val (currentState, setCurrentState) = useState<T?>(null)

    useBackgroundEffect(
        *values,
        state = state,
        cancelWhenBelowState = cancelWhenBelowState,
        cancelWhenRerun = cancelWhenRerun
    ) {
        setCurrentState(block())
    }

    return currentState
}
