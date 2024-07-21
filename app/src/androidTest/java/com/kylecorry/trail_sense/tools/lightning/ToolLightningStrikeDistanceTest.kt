package com.kylecorry.trail_sense.tools.lightning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.view
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
    fun basicFunctionality() {
        // Verify it is ready to record lightning
        view(R.id.start_btn).hasText(R.string.lightning)

        // Click the start button
        view(R.id.start_btn).click()

        // Verify it is recording
        view(R.id.lightning_title).hasText(Regex("([0-9.]+) (ft|mi)"))
        view(R.id.start_btn).hasText(R.string.thunder)

        // Click the start button again
        view(R.id.start_btn).click()

        // Verify it recorded the distance
        var previousDistance: String? = null
        view(R.id.lightning_title).hasText {
            val regex = Regex("([0-9.]+) (ft|mi)")
            if (regex.matches(it)) {
                previousDistance = it
                true
            } else {
                false
            }
        }
        view(R.id.start_btn).hasText(R.string.lightning)
        view(R.id.lightning_title).hasText(R.string.lightning_dangerously_close)

        // Run it again and verify the last distance shows at the bottom of the screen
        view(R.id.start_btn)
            .click()
            .click()

        view(R.id.previous_strike).hasText(Regex("Last strike at [0-9]+:[0-9]+ [AP]M - $previousDistance"))
    }
}