package com.kylecorry.trail_sense.tools.waterpurification

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import com.kylecorry.trail_sense.tools.waterpurification.ui.WaterPurificationFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
class WaterPurificationFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: FragmentScenario<WaterPurificationFragment>
    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        scenario = FragmentScenario.launchInContainer(
            WaterPurificationFragment::class.java,
            themeResId = R.style.AppTheme
        )
        context = InstrumentationRegistry.getInstrumentation().targetContext
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
        onView(withId(R.id.time_left)).check(matches(withText("59")))
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
    fun canSelectDifferentTimes(){
        // Select 1 minute
        onView(withId(R.id.chip_1_min)).perform(click())
        onView(withId(R.id.time_left)).check(matches(withText("60")))

        // Select 3 minutes
        onView(withId(R.id.chip_3_min)).perform(click())
        onView(withId(R.id.time_left)).check(matches(withText("180")))
    }

    @Test
    fun canAutomaticallySelectTime(){
        onView(withId(R.id.chip_auto)).perform(click())
        // TODO: Mock out GPS elevation and verify with different elevations (and wait for it to load)
    }
}