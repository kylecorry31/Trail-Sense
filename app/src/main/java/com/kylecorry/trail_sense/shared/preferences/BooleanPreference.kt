package com.kylecorry.trail_sense.shared.preferences

import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import kotlin.reflect.KProperty

class BooleanPreference(
    private val cache: Cache,
    private val name: String,
    private val defaultValue: Boolean
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return cache.getBoolean(name) ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        cache.putBoolean(name, value)
    }

}