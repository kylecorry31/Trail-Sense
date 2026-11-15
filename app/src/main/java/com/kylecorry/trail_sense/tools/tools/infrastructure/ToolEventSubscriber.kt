package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.os.Bundle
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.IEventSubscriber

object ToolEventSubscriber : IEventSubscriber {

    override fun subscribe(eventId: String, callback: suspend (EventData) -> Unit) {
        Tools.subscribe(eventId) { callback(it.toEventData()) }
    }

    @Suppress("DEPRECATION")
    private fun Bundle.toEventData(): EventData {
        val data = EventData()
        keySet().forEach { key ->
            when (val value = get(key)) {
                is String -> data.putString(key, value)
                is Int -> data.putInt(key, value)
                is Long -> data.putLong(key, value)
                is Float -> data.putFloat(key, value)
                null -> data.putString(key, null)
            }
        }
        return data
    }
}
