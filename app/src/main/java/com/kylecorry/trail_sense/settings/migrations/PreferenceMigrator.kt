package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache

class PreferenceMigrator private constructor() {

    private val lock = Object()

    fun migrate(context: Context) {
        synchronized(lock) {
            val cache = Cache(context)
            if (cache.contains("pref_enable_experimental")) {
                val isExperimental = cache.getBoolean("pref_enable_experimental") ?: false
                val useCamera = cache.getBoolean("pref_use_camera_features") ?: false
                cache.putBoolean(context.getString(R.string.pref_experimental_maps), isExperimental)
                cache.putBoolean(
                    context.getString(R.string.pref_experimental_tide_clock),
                    isExperimental
                )
                cache.putBoolean(
                    context.getString(R.string.pref_experimental_sighting_compass),
                    isExperimental && useCamera
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