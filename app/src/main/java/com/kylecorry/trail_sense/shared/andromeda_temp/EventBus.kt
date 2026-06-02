package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.luna.subscriptions.generic.Subscription

class EventBus<T> {
    private val subscriptions = mutableMapOf<String, Subscription<T>>()
    private val subscriptionLock = Any()

    fun publish(topicId: String, value: T) {
        val subscription = synchronized(subscriptionLock) {
            subscriptions[topicId]
        }
        subscription?.publish(value)
    }

    fun subscribe(topicId: String, callback: suspend (T) -> Unit) {
        synchronized(subscriptionLock) {
            val topic = subscriptions.getOrPut(topicId) {
                Subscription()
            }
            topic.subscribe(callback)
        }
    }

    fun unsubscribe(topicId: String, callback: suspend (T) -> Unit) {
        synchronized(subscriptionLock) {
            val topic = subscriptions[topicId]
            topic?.unsubscribe(callback)
        }
    }
}
