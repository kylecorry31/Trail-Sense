package com.kylecorry.trail_sense.tools.light

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input

@HiltAndroidTest
class ToolLightMeterTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setupApplication()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.startWithTool(Tools.LIGHT_METER)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.light_title).hasText(Regex("\\d+ lx"))
        }

        // Enter the distance
        view(R.id.amount).input("10")

        // The candella and beam distance should be shown
        waitFor {
            view(R.id.light_title).hasText(Regex("\\d+ cd"))
            view(R.id.beam_distance_text).hasText(Regex("\\d+ ft beam distance"))
        }

        // Reset
        view(R.id.reset_btn).click()
        waitFor {
            view(R.id.light_title).hasText(Regex("\\d+ cd"))
            view(R.id.beam_distance_text).hasText(Regex("\\d+ ft beam distance"))
        }
    }
}