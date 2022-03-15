package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class BacktrackRequiresForegroundSpecification : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val prefs = UserPreferences(value)

        val isAlwaysOn = prefs.backtrackRecordFrequency < Duration.ofMinutes(15)

        val barometerModes = listOf(UserPreferences.AltimeterMode.Barometer, UserPreferences.AltimeterMode.GPSBarometer)
        val usesBarometer = prefs.altimeterMode in barometerModes && Sensors.hasBarometer(value)

        if (isAlwaysOn || usesBarometer) {
            return true
        }

        val usesOverride = !prefs.useAutoLocation
        val noLocationAccess = !Permissions.canGetFineLocation(value)
        val noGps = !GPS.isAvailable(value)

        if (usesOverride || noLocationAccess || noGps) {
            return false
        }

        return !Permissions.isBackgroundLocationEnabled(value)
    }
}