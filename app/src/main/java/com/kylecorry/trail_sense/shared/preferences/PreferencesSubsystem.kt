package com.kylecorry.trail_sense.shared.preferences

import android.content.Context
import com.kylecorry.andromeda.preferences.CachedPreferences
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.preferences.Preferences

class PreferencesSubsystem private constructor(context: Context) {
    val preferences: IPreferences = CachedPreferences(Preferences(context))

    companion object {
        private var instance: PreferencesSubsystem? = null
        fun getInstance(context: Context): PreferencesSubsystem {
            if (instance == null) {
                instance = PreferencesSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}