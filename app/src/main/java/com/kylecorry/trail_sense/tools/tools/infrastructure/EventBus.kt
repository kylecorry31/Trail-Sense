package com.kylecorry.trail_sense.tools.tools.infrastructure

class EventBus<T> {

    private val subscribers = mutableMapOf<String, MutableSet<suspend (T) -> Unit>>()

    suspend fun publish(eventId: String, value: T) {
        val snapshot = synchronized(subscribers) {
            subscribers[eventId]?.toList() ?: emptyList()
        }

        snapshot.forEach {
            it(value)
        }
    }

    fun subscribe(eventId: String, subscriber: suspend (T) -> Unit) {
        synchronized(subscribers) {
            subscribers.getOrPut(eventId) {
                mutableSetOf()
            }.add(subscriber)
        }
    }

    fun unsubscribe(eventId: String, subscriber: suspend (T) -> Unit) {
        synchronized(subscribers) {
            subscribers[eventId]?.remove(subscriber)
        }
    }
}
