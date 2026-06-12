package com.kylecorry.trail_sense.shared.events

interface IEventEmitter {
    fun broadcast(eventId: String, data: EventData? = null)
}
