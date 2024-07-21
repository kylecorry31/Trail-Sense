package com.kylecorry.trail_sense.tools.whistle

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.WHISTLE)
    }

    @Test
    fun basicFunctionality() {
        waitFor {
            view(R.id.whistle_btn)
        }

        // TODO: Mute to avoid playing the sound

        // Press the whistle buttons
        view(R.id.whistle_btn).click(200)

        // The other buttons are toggles, so turn them on and off
        view(R.id.whistle_sos_btn)
            .click()
            .click()

        view(R.id.whistle_emergency_btn)
            .click()
            .click()

        // TODO: Verify sound is played
    }
}