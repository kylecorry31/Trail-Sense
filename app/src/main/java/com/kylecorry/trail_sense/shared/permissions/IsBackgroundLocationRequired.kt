package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService

class IsBackgroundLocationRequired: Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val sensorService = SensorService(value)
        val hasForegroundLocation = sensorService.hasLocationPermission()
        val hasBackgroundLocation = sensorService.hasLocationPermission(true)

        if (!hasForegroundLocation || hasBackgroundLocation) {
            return false
        }

        val prefs = UserPreferences(value)
        return prefs.astronomy.sendSunsetAlerts
    }
}