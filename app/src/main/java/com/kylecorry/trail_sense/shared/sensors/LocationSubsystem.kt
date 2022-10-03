package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS

class LocationSubsystem private constructor(private val context: Context) {

    private val cache by lazy { CachedGPS(context) }
    private val override by lazy { OverrideGPS(context) }

    val location: Coordinate
        get() = if (isGPSOverridden()) override.location else cache.location

    private val userPrefs by lazy { UserPreferences(context) }

    private fun isGPSOverridden(): Boolean {
        if (!userPrefs.useAutoLocation || !Permissions.canGetFineLocation(context)) {
            return true
        }

        return false
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: LocationSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): LocationSubsystem {
            if (instance == null) {
                instance = LocationSubsystem(context.applicationContext)
            }
            return instance!!
        }

    }

}