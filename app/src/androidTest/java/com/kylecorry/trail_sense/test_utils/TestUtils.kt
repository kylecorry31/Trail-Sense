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
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
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
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.rules.TestRule
import java.time.Duration

object TestUtils {

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

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

    // CLICK
    fun click(@IdRes id: Int, @IdRes childId: Int? = null) {
        perform(withId2(id, childId), ViewActions.click())
    }

    fun longClick(@IdRes id: Int, @IdRes childId: Int? = null) {
        perform(withId2(id, childId), ViewActions.longClick())
    }

    // TEXT
    fun hasText(
        @IdRes id: Int,
        @StringRes textResId: Int,
        @IdRes childId: Int? = null,
        checkDescendants: Boolean = false
    ) {
        checkMatch(withId2(id, childId), ViewMatchers.withText(textResId), checkDescendants)
    }

    fun hasText(
        @IdRes id: Int,
        text: String,
        @IdRes childId: Int? = null,
        checkDescendants: Boolean = false
    ) {
        checkMatch(withId2(id, childId), ViewMatchers.withText(text), checkDescendants)
    }

    fun hasText(
        @IdRes id: Int,
        @IdRes childId: Int? = null,
        checkDescendants: Boolean = false,
        predicate: (text: String) -> Boolean
    ) {
        checkMatch(withId2(id, childId), withText(predicate), checkDescendants)
    }

    // NOTIFICATIONS
    fun hasNotification(id: Int) {
        assertEquals(Notify.isActive(context, id), true)
    }

    fun doesNotHaveNotification(id: Int) {
        assertEquals(Notify.isActive(context, id), false)
    }

    // HELPERS
    private fun withId2(@IdRes parentId: Int, @IdRes childId: Int? = null): Matcher<View> {
        if (childId == null) {
            return withId(parentId)
        }
        return allOf(withId(childId), isDescendantOfA(withId(parentId)))
    }

    private fun checkMatch(
        viewMatcher: Matcher<View>,
        matcher: Matcher<View>,
        checkDescendants: Boolean = false
    ) {
        val actualMatcher = if (checkDescendants) {
            ViewMatchers.hasDescendant(matcher)
        } else {
            matcher
        }
        onView(viewMatcher).check(matches(actualMatcher))
    }

    private fun perform(viewMatcher: Matcher<View>, vararg actions: ViewAction) {
        onView(viewMatcher).perform(*actions)
    }

    private fun withPredicate(predicate: (view: View) -> Boolean) =
        object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("with predicate")
            }

            override fun matchesSafely(item: View): Boolean {
                return predicate(item)
            }
        }

    private fun withText(predicate: (text: String) -> Boolean) =
        withPredicate { view -> view is TextView && predicate(view.text.toString()) }
}