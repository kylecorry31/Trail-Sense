package com.kylecorry.trail_sense.tools.lightning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolLightningStrikeDistanceTest {

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
        TestUtils.startWithTool(Tools.LIGHTNING_STRIKE_DISTANCE)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Verify it is ready to record lightning
        hasText(R.id.start_btn, string(R.string.lightning))

        // Click the start button
        click(R.id.start_btn)

        // Verify it is recording
        hasText(R.id.lightning_title, Regex("([0-9.]+) (ft|mi)"))
        hasText(R.id.start_btn, string(R.string.thunder))

        // Click the start button again
        click(R.id.start_btn)

        // Verify it recorded the distance
        var previousDistance: String? = null
        hasText(R.id.lightning_title) {
            val regex = Regex("([0-9.]+) (ft|mi)")
            if (regex.matches(it)) {
                previousDistance = it
                true
            } else {
                false
            }
        }
        hasText(R.id.start_btn, string(R.string.lightning))
        hasText(R.id.lightning_title, string(R.string.lightning_dangerously_close))

        // Run it again and verify the last distance shows at the bottom of the screen
        click(R.id.start_btn)
        click(R.id.start_btn)

        hasText(R.id.previous_strike, Regex("Last strike at [0-9]+:[0-9]+ [AP]M - $previousDistance"))
    }
}