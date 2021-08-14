package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R

class PreferenceMigrator private constructor() {

    private val lock = Object()

    fun migrate(context: Context) {
        synchronized(lock) {
            val cache = Preferences(context)
            if (cache.contains("pref_enable_experimental")) {
                val isExperimental = cache.getBoolean("pref_enable_experimental") ?: false
                cache.putBoolean(context.getString(R.string.pref_experimental_maps), isExperimental)
                cache.putBoolean(
                    context.getString(R.string.pref_experimental_tide_clock),
                    isExperimental
                )
                cache.remove("pref_enable_experimental")
                cache.remove("pref_use_camera_features")
            }
        }
    }


    companion object {
        private var instance: PreferenceMigrator? = null
        private val staticLock = Object()

        fun getInstance(): PreferenceMigrator {
            return synchronized(staticLock) {
                if (instance == null) {
                    instance = PreferenceMigrator()
                }
                instance!!
            }
        }


    }

}