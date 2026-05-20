package com.kylecorry.trail_sense.shared.extensions.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.topics.Subscriber

@Composable
fun <T : ITopic, V> useTopic(
    topic: T,
    default: V,
    mapper: (T) -> V
): V {
    val (state, setState) = useState(default)

    useEffectWithCleanup(topic) {
        val subscriber: Subscriber = {
            setState(mapper(topic))
            true
        }

        if (topic is ISensor) {
            topic.start(subscriber)
        } else {
            topic.subscribe(subscriber)
        }

        return@useEffectWithCleanup {
            if (topic is ISensor) {
                topic.stop(subscriber)
            } else {
                topic.unsubscribe(subscriber)
            }
        }
    }

    return state
}

@Composable
fun <T : ITopic, V> useTopic(
    topic: T,
    mapper: (T) -> V?
): V? {
    return useTopic(topic, null, mapper)
}

@Composable
fun <T : Any, V> useTopic(
    topic: com.kylecorry.andromeda.core.topics.generic.ITopic<T>,
    default: V,
    mapper: (T) -> V
): V {
    val (state, setState) = useState(default)

    useEffectWithCleanup(topic) {
        val subscriber: com.kylecorry.andromeda.core.topics.generic.Subscriber<T> = {
            setState(mapper(it))
            true
        }

        topic.subscribe(subscriber)

        return@useEffectWithCleanup {
            topic.unsubscribe(subscriber)
        }
    }

    return state
}

@Composable
fun <T : Any, V> useTopic(
    topic: com.kylecorry.andromeda.core.topics.generic.ITopic<T>,
    mapper: (T) -> V?
): V? {
    return useTopic(topic, null, mapper)
}
