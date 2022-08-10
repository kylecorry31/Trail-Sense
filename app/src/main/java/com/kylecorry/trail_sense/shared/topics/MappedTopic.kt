package com.kylecorry.trail_sense.shared.topics

import com.kylecorry.andromeda.core.topics.generic.BaseTopic
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.Topic

internal class MappedTopic<T, V>(private val baseTopic: ITopic<T>, private val map: (T) -> V) :
    BaseTopic<V>() {

    override val topic = Topic.lazy<V>(
        { baseTopic.subscribe(this::onValue) },
        { baseTopic.unsubscribe(this::onValue) },
    )

    init {
        if (baseTopic.value.isPresent){
            onValue(baseTopic.value.get())
        }
    }

    private fun onValue(value: T): Boolean {
        topic.notifySubscribers(map(value))
        return true
    }
}

fun <T, V> ITopic<T>.map(fn: (T) -> V): ITopic<V> {
    return MappedTopic(this, fn)
}