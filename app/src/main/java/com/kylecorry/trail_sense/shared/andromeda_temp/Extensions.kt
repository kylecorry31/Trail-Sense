package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.log
import kotlin.math.sqrt
import kotlin.random.Random

fun <T> ReactiveComponent.useFlow(
    flow: Flow<T>,
    state: BackgroundMinimumState = BackgroundMinimumState.Created,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
): T? {
    val (value, setValue) = useState<T?>(null)
    useBackgroundEffect(
        state = state,
        repeat = true,
        cancelWhenRerun = false,
        cancelWhenBelowState = true
    ) {
        withContext(collectOn) {
            flow.collect {
                withContext(observeOn) {
                    setValue(it)
                }
            }
        }
    }
    return value
}

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