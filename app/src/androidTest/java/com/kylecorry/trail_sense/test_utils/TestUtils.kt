package com.kylecorry.trail_sense.test_utils

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.NotificationChannels
import com.kylecorry.trail_sense.main.automations.Automations
import com.kylecorry.trail_sense.main.persistence.RepoCleanupWorker
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import org.junit.rules.TestRule
import java.time.Duration

object TestUtils {

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }

    fun setWaitForIdleTimeout(timeout: Long) {
        Configurator.getInstance().setWaitForIdleTimeout(timeout)
    }

    fun not(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            return
        }
        throw Exception("Expected exception")
    }

    // STARTUP
    /**
     * Setup the application to match the actual application (Trail Sense application)
     */
    fun setupApplication(setDefaultPrefs: Boolean = true) {
        if (setDefaultPrefs) {
            setupDefaultPreferences()
        }
        Automations.setup(context)
        NotificationChannels.createChannels(context)
        PreferenceMigrator.getInstance().migrate(context)
        RepoCleanupWorker.scheduler(context).interval(Duration.ofHours(6))

        // Start up the weather subsystem
        WeatherSubsystem.getInstance(context)

        // Start up the flashlight subsystem
        FlashlightSubsystem.getInstance(context)
    }

    fun startWithTool(toolId: Long): ActivityScenario<MainActivity> {
        val prefs = UserPreferences(context)
        prefs.bottomNavigationTools = listOf(toolId)
        return ActivityScenario.launch(MainActivity::class.java)
    }

    // PERMISSIONS
    fun mainPermissionsGranted(): TestRule {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return GrantPermissionRule.grant(*permissions.toTypedArray())
    }

    fun allPermissionsGranted(): TestRule {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return GrantPermissionRule.grant(*permissions.toTypedArray())
    }

    // PREFERENCES
    fun setupDefaultPreferences() {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        prefs.putString(context.getString(R.string.pref_distance_units), "feet_miles")
        prefs.putBoolean(context.getString(R.string.pref_use_24_hour), false)
        prefs.putBoolean(context.getString(R.string.pref_onboarding_completed), true)
        prefs.putBoolean(context.getString(R.string.pref_main_disclaimer_shown_key), true)
        prefs.putBoolean(context.getString(R.string.pref_require_satellites), false)
    }

    // WAITING
    fun waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun <T> waitFor(durationMillis: Long = 5000, action: () -> T): T {
        var remaining = durationMillis
        val interval = 10L
        var lastException: Throwable? = null
        while (remaining > 0) {
            try {
                return action()
            } catch (e: Throwable) {
                lastException = e
            }
            Thread.sleep(interval)
            remaining -= interval
        }
        if (lastException != null) {
            throw lastException
        }
        throw Exception("Timeout")
    }

    // NOTIFICATIONS
    fun openNotificationShade() {
        device.openNotification()
    }

    fun closeNotificationShade() {
        device.swipe(0, device.displayHeight, 0, 0, 10)
    }

    // HELPERS
    fun resourceIdToName(id: Int): String {
        return context.resources.getResourceEntryName(id)
    }

    fun find(selector: BySelector): UiObject2? {
        return waitFor {
            device.findObject(selector)
        }
    }

    fun matchesSelfOrChild(
        parent: UiObject2,
        depth: Int = 10,
        predicate: (obj: UiObject2) -> Boolean
    ): Boolean {
        if (depth == 0) {
            return false
        }

        if (predicate(parent)) {
            return true
        }

        for (child in parent.children) {
            if (matchesSelfOrChild(child, depth - 1, predicate)) {
                return true
            }
        }

        return false
    }
}