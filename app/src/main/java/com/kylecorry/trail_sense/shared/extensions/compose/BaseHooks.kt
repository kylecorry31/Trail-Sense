package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kylecorry.luna.hooks.Ref
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

@Composable
fun useEffect(vararg values: Any?, action: suspend CoroutineScope.() -> Unit) {
    LaunchedEffect(*values) {
        action()
    }
}

@Composable
fun <T> useMemo(vararg values: Any?, value: () -> T): T {
    return remember(*values) { value() }
}

@Composable
fun <T> useState(initialValue: T): Pair<T, (T) -> Unit> {
    val state = remember { mutableStateOf(initialValue) }
    return Pair(state.value) { state.value = it }
}

@Composable
fun <T> useRef(initialValue: T): Ref<T> {
    return remember { Ref(initialValue) }
}

@Composable
fun useEffectWithCleanup(vararg values: Any?, action: () -> () -> Unit) {
    DisposableEffect(*values) {
        val cleanup = action()
        onDispose { cleanup() }
    }
}

@Composable
fun useTrigger(): Pair<String, () -> Unit> {
    val (key, setKey) = useState("")
    val trigger = useCallback<Unit> {
        setKey(UUID.randomUUID().toString())
    }
    return useMemo(key, trigger) { key to trigger }
}


// Callback
@Composable
fun <T> useCallback(vararg values: Any?, callback: () -> T): () -> T {
    return useMemo(*values) { callback }
}

@Composable
fun <R, T> useCallback(vararg values: Any?, callback: (R) -> T): (R) -> T {
    return useMemo(*values) { callback }
}

@Composable
fun <R, S, T> useCallback(
    vararg values: Any?,
    callback: (R, S) -> T
): (R, S) -> T {
    return useMemo(*values) { callback }
}

@Composable
fun <R, S, U, T> useCallback(
    vararg values: Any?,
    callback: (R, S, U) -> T
): (R, S, U) -> T {
    return useMemo(*values) { callback }
}

@Composable
fun <R, S, U, V, T> useCallback(
    vararg values: Any?,
    callback: (R, S, U, V) -> T
): (R, S, U, V) -> T {
    return useMemo(*values) { callback }
}

@Composable
fun <R, S, U, V, W, T> useCallback(
    vararg values: Any?,
    callback: (R, S, U, V, W) -> T
): (R, S, U, V, W) -> T {
    return useMemo(*values) { callback }
}
