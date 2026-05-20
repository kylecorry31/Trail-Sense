package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@Composable
fun <T> useFlow(
    flow: Flow<T>,
    vararg values: Any?,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
): T? {
    val (value, setValue) = useState<T?>(null)
    useEffect(
        *values
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
