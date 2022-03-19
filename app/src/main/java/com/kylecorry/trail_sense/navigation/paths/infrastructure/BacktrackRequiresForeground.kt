package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.UserPreferences

class BacktrackRequiresForeground : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val prefs = UserPreferences(value)

        val usesOverride = !prefs.useAutoLocation
        val noLocationAccess = !Permissions.canGetFineLocation(value)
        val noGps = !GPS.isAvailable(value)

        if (usesOverride || noLocationAccess || noGps) {
            return false
        }

        return !Permissions.isBackgroundLocationEnabled(value)
    }
}