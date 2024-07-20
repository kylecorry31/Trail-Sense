package com.kylecorry.trail_sense.test_utils

import android.Manifest
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kylecorry.andromeda.notify.Notify
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
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.rules.TestRule
import java.time.Duration

object TestUtils {

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    fun setupDefaultPreferences() {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        prefs.putString(context.getString(R.string.pref_distance_units), "feet_miles")
        prefs.putBoolean(context.getString(R.string.pref_use_24_hour), false)
        prefs.putBoolean(context.getString(R.string.pref_onboarding_completed), true)
        prefs.putBoolean(context.getString(R.string.pref_main_disclaimer_shown_key), true)
        prefs.putBoolean(context.getString(R.string.pref_require_satellites), false)
    }

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

    fun withText(predicate: (text: String) -> Boolean) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with text (predicate)")
        }

        override fun matchesSafely(item: View): Boolean {
            if (item !is TextView) return false
            val text = item.text.toString()
            return predicate(text)
        }
    }

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

    fun waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun waitFor(durationMillis: Long = 5000, action: () -> Unit) {
        var remaining = durationMillis
        val interval = 100L
        var lastException: Throwable? = null
        while (remaining > 0) {
            try {
                action()
                return
            } catch (e: Throwable) {
                lastException = e
            }
            Thread.sleep(interval)
            remaining -= interval
        }
        if (lastException != null) {
            throw lastException
        }
    }

    fun waitForMatch(matcher: Matcher<View>, durationMillis: Long = 5000) =
        object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("wait for matcher")
            }

            override fun matchesSafely(item: View): Boolean {
                var remaining = durationMillis
                val interval = 100L
                while (remaining > 0) {
                    if (matcher.matches(item)) {
                        return true
                    }
                    Thread.sleep(interval)
                    remaining -= interval
                }
                return matcher.matches(item)
            }
        }

    fun click(@IdRes id: Int) {
        onView(withId(id)).perform(ViewActions.click())
    }

    fun hasText(@IdRes id: Int, @StringRes textResId: Int, checkDescendants: Boolean = false) {
        if (checkDescendants) {
            onView(withId(id)).check(
                matches(ViewMatchers.hasDescendant(ViewMatchers.withText(textResId)))
            )
        } else {
            onView(withId(id)).check(matches(ViewMatchers.withText(textResId)))
        }
    }

    fun hasText(@IdRes id: Int, text: String, checkDescendants: Boolean = false) {
        if (checkDescendants) {
            onView(withId(id)).check(
                matches(ViewMatchers.hasDescendant(ViewMatchers.withText(text)))
            )
        } else {
            onView(withId(id)).check(matches(ViewMatchers.withText(text)))
        }
    }

    fun hasText(
        @IdRes id: Int,
        checkDescendants: Boolean = false,
        predicate: (text: String) -> Boolean
    ) {
        if (checkDescendants) {
            onView(withId(id)).check(matches(ViewMatchers.hasDescendant(withText(predicate))))
        } else {
            onView(withId(id)).check(matches(withText(predicate)))
        }
    }

    fun hasNotification(id: Int) {
        assertEquals(Notify.isActive(context, id), true)
    }

    fun doesNotHaveNotification(id: Int) {
        assertEquals(Notify.isActive(context, id), false)
    }

}