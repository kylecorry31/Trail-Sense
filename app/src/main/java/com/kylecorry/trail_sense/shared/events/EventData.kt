package com.kylecorry.trail_sense.shared.events

class EventData {

    private val values = mutableMapOf<String, Any?>()

    fun putString(key: String, value: String?) {
        values[key] = value
    }

    fun putInt(key: String, value: Int) {
        values[key] = value
    }

    fun putLong(key: String, value: Long) {
        values[key] = value
    }

    fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    fun forEach(action: (key: String, value: Any?) -> Unit) {
        values.forEach(action)
    }
}
