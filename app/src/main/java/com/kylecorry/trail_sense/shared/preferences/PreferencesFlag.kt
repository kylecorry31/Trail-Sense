package com.kylecorry.trail_sense.shared.preferences

import com.kylecorry.andromeda.preferences.IPreferences

class PreferencesFlag(
    private val prefs: IPreferences,
    private val key: String,
    private val defaultValue: Boolean = false
) : Flag {

    override fun set(shown: Boolean) {
        prefs.putBoolean(key, shown)
    }

    override fun get(): Boolean {
        return prefs.getBoolean(key) ?: defaultValue
    }
}