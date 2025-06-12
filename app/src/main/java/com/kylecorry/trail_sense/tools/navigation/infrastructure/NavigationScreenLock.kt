package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.app.Activity
import androidx.navigation.NavController
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.findNavController
import java.lang.ref.WeakReference

class NavigationScreenLock(private val alwaysLock: Boolean = false) {

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val shouldLock by lazy { prefs.navigation.keepScreenUnlockedWhileNavigating }
    private val navigator = AppServiceRegistry.get<Navigator>()

    private var activityReference: WeakReference<Activity>? = null
    private var selfDestinationId: Int? = null
    private val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (destination.id != selfDestinationId) {
            releaseLock(activityReference?.get() ?: return@OnDestinationChangedListener)
        }
    }
    private val removeListenerTimer = CoroutineTimer {
        tryOrNothing {
            activityReference?.get()?.let {
                removeNavigationListener(it)
            }
        }
    }

    fun updateLock(activity: Activity) {
        activityReference = WeakReference(activity)
        tryOrNothing {
            val shouldShowWhenLocked = alwaysLock || (shouldLock && navigator.isNavigating())
            if (shouldShowWhenLocked) {
                Screen.setShowWhenLocked(activity, true)
                addNavigationListener(activity)
            } else {
                releaseLock(activity)
            }
        }
    }

    fun releaseLock(activity: Activity) {
        tryOrNothing {
            Screen.setShowWhenLocked(activity, false)
            removeListenerTimer.once(1000)
        }
    }

    private fun removeNavigationListener(activity: Activity) {
        val mainActivity = activity as? MainActivity ?: return
        val navController = mainActivity.findNavController()
        navController.removeOnDestinationChangedListener(listener)
    }

    private fun addNavigationListener(activity: Activity) {
        val weakActivity = WeakReference(activity)
        val mainActivity = weakActivity.get() as? MainActivity ?: return
        val navController = mainActivity.findNavController()
        selfDestinationId = navController.currentDestination?.id
        navController.addOnDestinationChangedListener(listener)
    }

}