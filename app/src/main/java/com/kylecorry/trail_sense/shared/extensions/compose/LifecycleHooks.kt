package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun useLifecycleOwner(): LifecycleOwner {
    return LocalLifecycleOwner.current
}

@Composable
fun useLifecycleEffect(
    lifecycleEvent: Lifecycle.Event,
    vararg values: Any?,
    action: () -> Unit
) {
    val owner = useLifecycleOwner()
    val (lastObserver, setLastObserver) = useState<LifecycleObserver?>(null)
    val observer = useMemo(*values, lifecycleEvent) {
        LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            if (event == lifecycleEvent) {
                action()
            }
        }
    }

    useEffect(owner, observer) {
        setLastObserver(observer)
        lastObserver?.let {
            owner.lifecycle.removeObserver(it)
        }
        owner.lifecycle.addObserver(observer)
    }
}

@Composable
fun usePauseEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_PAUSE,
        *values
    ) {
        action()
    }
}

@Composable
fun useResumeEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_RESUME,
        *values
    ) {
        action()
    }
}

@Composable
fun useDestroyEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_DESTROY,
        *values
    ) {
        action()
    }
}
