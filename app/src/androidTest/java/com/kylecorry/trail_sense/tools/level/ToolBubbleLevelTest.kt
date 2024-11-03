package com.kylecorry.trail_sense.tools.level

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolBubbleLevelTest {

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
        TestUtils.startWithTool(Tools.BUBBLE_LEVEL)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Verify it shows the current angle
        hasText(
            R.id.level_title,
            Regex(
                string(
                    R.string.bubble_level_angles,
                    "\\d+\\.\\d°",
                    "\\d+\\.\\d°"
                )
            )
        )

        // Verify the level is shown (will fail if the level is not visible)
        isVisible(R.id.level)
    }
}