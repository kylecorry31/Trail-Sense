package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.os.Bundle
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.IEventEmitter

object ToolEventEmitter : IEventEmitter {

    override fun broadcast(eventId: String, data: EventData?) {
        Tools.broadcast(eventId, data?.toBundle())
    }

    private fun EventData.toBundle(): Bundle {
        val bundle = Bundle()
        forEach { key, value ->
            when (value) {
                null -> bundle.putString(key, null)
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
            }
        }
        return bundle
    }
}
