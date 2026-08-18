package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.preferences.Preference
import com.kylecorry.luna.topics.generic.Topic
import com.kylecorry.sol.units.Coordinate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class InMemoryPreferences : IPreferences {

    private val values = mutableMapOf<String, Any>()

    override val onChange = Topic<String>()

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun contains(key: String): Boolean = values.containsKey(key)

    private inline fun <reified T> get(key: String): T? = values[key] as? T

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getInt(key: String): Int? = get(key)

    override fun getBoolean(key: String): Boolean? = get(key)

    override fun getString(key: String): String? = get(key)

    override fun getFloat(key: String): Float? = get(key)

    override fun getDouble(key: String): Double? = get(key)

    override fun getLong(key: String): Long? = get(key)

    override fun putCoordinate(key: String, value: Coordinate) {
        values[key] = value
    }

    override fun getCoordinate(key: String): Coordinate? = get(key)

    override fun getLocalDate(key: String): LocalDate? = get(key)

    override fun putLocalDate(key: String, date: LocalDate) {
        values[key] = date
    }

    override fun putInstant(key: String, value: Instant) {
        values[key] = value
    }

    override fun getInstant(key: String): Instant? = get(key)

    override fun getDuration(key: String): Duration? = get(key)

    override fun putDuration(key: String, duration: Duration) {
        values[key] = duration
    }

    override fun getAll(): Collection<Preference> = emptyList()

    override fun putAll(preferences: Collection<Preference>, clearOthers: Boolean) {
        // Not needed by the migrator
    }

    override fun clear() {
        values.clear()
    }

    override fun close() {
        // Nothing to close
    }
}
