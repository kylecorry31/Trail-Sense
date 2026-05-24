package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.inBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@Composable
fun <T> useFlow(
    flow: Flow<T>,
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
): T? {
    val (value, setValue) = useState<T?>(null)
    val owner = useLifecycleOwner()
    useEffectWithCleanup(
        *values,
        flow,
        owner,
        state,
        cancelWhenBelowState,
        collectOn,
        observeOn
    ) {
        val job = owner.inBackground(state, cancelWhenBelowState) {
            withContext(collectOn) {
                flow.collect {
                    withContext(observeOn) {
                        setValue(it)
                    }
                }
            }
        }
        return@useEffectWithCleanup {
            job.cancel()
        }
    }
    return value
}
