package com.kylecorry.trail_sense.tools.waterpurification

import android.Manifest
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class WaterPurificationFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule
    val grantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(*permissions, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant(*permissions)
    }

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val prefs = PreferencesSubsystem.getInstance(context).preferences
        val userPrefs = UserPreferences(context)
        prefs.putBoolean(context.getString(R.string.pref_onboarding_completed), true)
        prefs.putBoolean(context.getString(R.string.pref_main_disclaimer_shown_key), true)
        prefs.putBoolean(context.getString(R.string.pref_require_satellites), false)
        userPrefs.bottomNavigationTools = listOf(Tools.WATER_BOIL_TIMER)

        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun canStartTimer() {
        // Select 1 minute
        onView(withId(R.id.chip_1_min)).perform(click())
        onView(withId(R.id.time_left)).check(matches(withText("60")))

        // Start the timer
        onView(withId(R.id.boil_button)).perform(click())

        // Verify it is started
        onView(withId(R.id.boil_button)).check(matches(withText(android.R.string.cancel)))
        onView(withId(R.id.time_left)).check(matches(withTextValueLessThanOrEqualTo(60)))
        assertEquals(Notify.isActive(context, WaterPurificationTimerService.NOTIFICATION_ID), true)

        // TODO: Wait for the timer to finish and verify the finished state (simulate time passing)

        // Cancel the timer
        onView(withId(R.id.boil_button)).perform(click())

        // Verify it is stopped
        onView(withId(R.id.boil_button)).check(matches(withText(R.string.start)))
        onView(withId(R.id.time_left)).check(matches(withText("60")))
        assertEquals(Notify.isActive(context, WaterPurificationTimerService.NOTIFICATION_ID), false)
    }

    @Test
    fun canSelectDifferentTimes() {
        // Auto
        // TODO: Mock out elevation
        onView(withId(R.id.time_left)).check(matches(withText("180")))

        // Select 1 minute
        onView(withId(R.id.chip_1_min)).perform(click())
        onView(withId(R.id.time_left)).check(matches(withText("60")))

        // Select 3 minutes
        onView(withId(R.id.chip_3_min)).perform(click())
        onView(withId(R.id.time_left)).check(matches(withText("180")))
    }

    private fun withTextValueLessThanOrEqualTo(maxValue: Int) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with text value less than or equal to: $maxValue")
        }

        override fun matchesSafely(item: View): Boolean {
            if (item !is TextView) return false
            val text = item.text.toString()
            val value = text.toIntOrNull()
            return value != null && value <= maxValue
        }
    }
}