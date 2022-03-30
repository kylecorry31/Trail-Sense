package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class BacktrackRequiresForeground : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val prefs = UserPreferences(value)

        val recordsCellSignal = prefs.backtrackSaveCellHistory
        if (recordsCellSignal) {
            return true
        }

        val usesOverride = !prefs.useAutoLocation
        val noLocationAccess = !Permissions.canGetFineLocation(value)
        val noGps = !GPS.isAvailable(value)
        val alwaysOn = prefs.backtrackRecordFrequency < Duration.ofMinutes(15)

        if (usesOverride || noLocationAccess || noGps || alwaysOn) {
            return false
        }

        return !Permissions.isBackgroundLocationEnabled(value)
    }
}