package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

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