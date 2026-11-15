package com.kylecorry.trail_sense.shared.events

interface IEventSubscriber {
    fun subscribe(eventId: String, callback: suspend (EventData) -> Unit)
}
