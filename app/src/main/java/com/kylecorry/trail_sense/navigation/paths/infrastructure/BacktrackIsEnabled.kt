package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.canRunLocationForegroundService

class BacktrackIsEnabled : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val prefs = UserPreferences(value)
        val hasPermission = Permissions.canRunLocationForegroundService(value)
        return hasPermission && prefs.backtrackEnabled && BacktrackIsAvailable().isSatisfiedBy(value)
    }
}