package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.app.Activity
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.shared.UserPreferences

class NavigationScreenLock(private val alwaysLock: Boolean = false) {

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val shouldLock by lazy { prefs.navigation.keepScreenUnlockedWhileNavigating }
    private val navigator = AppServiceRegistry.get<Navigator>()

    fun updateLock(activity: Activity) {
        tryOrNothing {
            Screen.setShowWhenLocked(
                activity,
                alwaysLock || (shouldLock && navigator.isNavigating())
            )
        }
    }

    fun releaseLock(activity: Activity) {
        tryOrNothing {
            Screen.setShowWhenLocked(activity, false)
        }
    }
}