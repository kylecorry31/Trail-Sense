package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.luna.timer.TimerActionBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

@Composable
fun <T> useBackgroundMemo(
    vararg values: Any?,
    block: suspend CoroutineScope.() -> T
): T? {
    val (currentState, setCurrentState) = useState<T?>(null)

    useEffect(*values) {
        setCurrentState(block())
    }

    return currentState
}

@Composable
fun useCoroutineQueue(
    queueSize: Int = 1,
    ignoreExceptions: Boolean = false
): CoroutineQueueRunner {
    return useMemo {
        CoroutineQueueRunner(queueSize, ignoreExceptions = ignoreExceptions)
    }
}

// Callback

@Composable
fun useBackgroundCallback(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    callback: suspend CoroutineScope.() -> Unit
): () -> Job {
    val owner = useLifecycleOwner()
    return useMemo(*values) { { owner.inBackground(state, cancelWhenBelowState, false, callback) } }
}

@Composable
fun <R> useBackgroundCallback(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    callback: suspend CoroutineScope.(R) -> Unit
): (R) -> Job {
    val owner = useLifecycleOwner()
    return useMemo(*values) {
        { p1 ->
            owner.inBackground(state, cancelWhenBelowState) {
                callback(p1)
            }
        }
    }
}

@Composable
fun <R, S> useBackgroundCallback(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    callback: suspend CoroutineScope.(R, S) -> Unit
): (R, S) -> Job {
    val owner = useLifecycleOwner()
    return useMemo(*values) {
        { p1, p2 ->
            owner.inBackground(state, cancelWhenBelowState) {
                callback(p1, p2)
            }
        }
    }
}

@Composable
fun <R, S, U> useBackgroundCallback(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    callback: suspend CoroutineScope.(R, S, U) -> Unit
): (R, S, U) -> Job {
    val owner = useLifecycleOwner()

    return useMemo(*values) {
        { p1, p2, p3 ->
            owner.inBackground(state, cancelWhenBelowState) {
                callback(p1, p2, p3)
            }
        }
    }
}

@Composable
fun <R, S, U, V> useBackgroundCallback(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    callback: suspend CoroutineScope.(R, S, U, V) -> Unit
): (R, S, U, V) -> Job {
    val owner = useLifecycleOwner()

    return useMemo(*values) {
        { p1, p2, p3, p4 ->
            owner.inBackground(
                state,
                cancelWhenBelowState,
            ) {
                callback(p1, p2, p3, p4)
            }
        }
    }
}

@Composable
fun <R, S, U, V, W> useBackgroundCallback(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    callback: suspend CoroutineScope.(R, S, U, V, W) -> Unit
): (R, S, U, V, W) -> Job {
    val owner = useLifecycleOwner()

    return useMemo(*values) {
        { p1, p2, p3, p4, p5 ->
            owner.inBackground(
                state,
                cancelWhenBelowState,
            ) {
                callback(p1, p2, p3, p4, p5)
            }
        }
    }
}

@Composable
fun useTimer(
    interval: Long,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    observeOn: CoroutineContext = Dispatchers.Main,
    actionBehavior: TimerActionBehavior = TimerActionBehavior.Wait,
    autostart: Boolean = true,
    runnable: suspend () -> Unit
): Pair<() -> Unit, () -> Unit> {
    val shouldRun = useRef(autostart)
    val owner = useLifecycleOwner()
    val timer = useMemo(scope, observeOn, actionBehavior) {
        CoroutineTimer(scope, observeOn, actionBehavior) {
            runnable()
        }
    }

    val stop = useCallback<Unit>(timer, shouldRun) {
        shouldRun.current = false
        timer.stop()
    }

    val start = useCallback<Unit>(timer, shouldRun, interval) {
        shouldRun.current = true
        timer.interval(interval)
    }

    useEffect(owner, timer, interval) {
        if (shouldRun.current && owner.lifecycle.currentState == Lifecycle.State.RESUMED) {
            timer.interval(interval)
        }
    }

    useResumeEffect(timer, interval) {
        if (shouldRun.current) {
            timer.interval(interval)
        }
    }

    usePauseEffect(timer) {
        timer.stop()
    }

    useEffectWithCleanup(timer) {
        return@useEffectWithCleanup {
            timer.stop()
        }
    }

    return stop to start
}
