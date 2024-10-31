package com.kylecorry.trail_sense.tools.ruler

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolRulerTest {

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
        TestUtils.startWithTool(Tools.RULER)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.ruler)
        }

        // Click on the ruler
        view(R.id.ruler).click()

        // Verify amount
        view(R.id.ruler_unit_btn).hasText("in")
        view(R.id.measurement).hasText(Regex("\\d+\\.\\d+ in"))

        // Switch to CM
        view(R.id.ruler_unit_btn).click()
        view(R.id.ruler_unit_btn).hasText("cm")
        view(R.id.measurement).hasText(Regex("\\d+\\.\\d+ cm"))

        // Enter a to ratio
        view(R.id.fractional_map_to).input("10")
        view(R.id.fractional_map_from).hasText("1")
        view(R.id.map_distance).hasText(Regex("Map: \\d+\\.\\d+ m"))

        // Switch to verbal scale
        view(R.id.map_verbal_btn).click()
        view(R.id.verbal_map_scale_from, R.id.amount).input("1")
        view(R.id.verbal_map_scale_to, R.id.amount).input("10")
        view(R.id.map_distance).hasText(Regex("Map: \\d+\\.\\d+ ft"))

        // Switch back to ratio and verify it's still there
        view(R.id.map_ratio_btn).click()
        view(R.id.fractional_map_from).hasText("1")
        view(R.id.fractional_map_to).hasText("10")
        view(R.id.map_distance).hasText(Regex("Map: \\d+\\.\\d+ m"))

        verifyQuickActions()
    }

    private fun verifyQuickActions() {
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_RULER)
            .click()
            .click()

        TestUtils.closeQuickActions()
    }
}