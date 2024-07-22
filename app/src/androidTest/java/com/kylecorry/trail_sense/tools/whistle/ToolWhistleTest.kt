package com.kylecorry.trail_sense.tools.whistle

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@HiltAndroidTest
class ToolWhistleTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private var currentVolume = 0

    @Before
    fun setUp() {
        hiltRule.inject()
        currentVolume = TestUtils.mute()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.WHISTLE)

        // Wait for the tool to open
        waitFor {
            view(R.id.whistle_btn)
        }
    }

    @Test
    fun verifyBasicFunctionality() {
        // Whistle button (needs to press and hold)
        view(R.id.whistle_btn).click(200)

        // SOS button
        view(R.id.whistle_sos_btn)
            .click()

        waitFor {
            assertTrue(TestUtils.isPlayingMusic())
        }

        view(R.id.whistle_sos_btn)
            .click()

        waitFor {
            assertFalse(TestUtils.isPlayingMusic())
        }

        // Emergency button
        view(R.id.whistle_emergency_btn)
            .click()

        waitFor {
            assertTrue(TestUtils.isPlayingMusic())
        }

        view(R.id.whistle_emergency_btn)
            .click()


        waitFor {
            assertFalse(TestUtils.isPlayingMusic())
        }

        verifyQuickAction()
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_WHISTLE)
            .click(200)
        TestUtils.closeQuickActions()
    }

    @After
    fun tearDown() {
        TestUtils.unmute(currentVolume)
    }
}