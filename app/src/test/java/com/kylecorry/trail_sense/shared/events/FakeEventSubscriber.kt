package com.kylecorry.trail_sense.shared.events

import kotlinx.coroutines.runBlocking

class FakeEventSubscriber : IEventSubscriber {
    private val callbacks = mutableMapOf<String, MutableList<suspend (EventData) -> Unit>>()

    override fun subscribe(eventId: String, callback: suspend (EventData) -> Unit) {
        callbacks.getOrPut(eventId) { mutableListOf() }.add(callback)
    }

    fun publish(eventId: String, data: EventData = EventData()) {
        callbacks[eventId]?.forEach { runBlocking { it(data) } }
    }
}
