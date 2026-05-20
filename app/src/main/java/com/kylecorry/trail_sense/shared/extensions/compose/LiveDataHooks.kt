package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData

@Composable
fun <T : Any, V> useLiveData(
    data: LiveData<T>,
    default: V,
    mapper: (T) -> V
): V {
    val (state, setState) = useState(default)
    val owner = useLifecycleOwner()

    useEffectWithCleanup(data, owner) {
        val observer = androidx.lifecycle.Observer<T> {
            setState(mapper(it))
        }
        data.observe(owner, observer)

        return@useEffectWithCleanup {
            data.removeObserver(observer)
        }
    }

    return state
}

@Composable
fun <T : Any, V> useLiveData(
    data: LiveData<T>,
    mapper: (T) -> V?
): V? {
    return useLiveData(data, null, mapper)
}
