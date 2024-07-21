package com.kylecorry.trail_sense.tools.lightning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
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
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.LIGHTNING_STRIKE_DISTANCE)
    }

    @Test
    fun basicFunctionality() {
        // Verify it is ready to record lightning
        TestUtils.hasText(R.id.start_btn, R.string.lightning, checkDescendants = true)

        // Click the start button
        TestUtils.click(R.id.start_btn)

        // Verify it is recording
        TestUtils.hasText(
            R.id.lightning_title,
            checkDescendants = true
        ) {
            val regex = Regex("([0-9.]+) (ft|mi)")
            regex.matches(it)
        }
        TestUtils.hasText(R.id.start_btn, R.string.thunder, checkDescendants = true)

        // Click the start button again
        TestUtils.click(R.id.start_btn)

        // Verify it recorded the distance
        var previousDistance: String? = null
        TestUtils.hasText(
            R.id.lightning_title,
            checkDescendants = true
        ) {
            val regex = Regex("([0-9.]+) (ft|mi)")
            if (regex.matches(it)){
                previousDistance = it
                true
            } else {
                false
            }
        }
        TestUtils.hasText(R.id.start_btn, R.string.lightning, checkDescendants = true)
        TestUtils.hasText(R.id.lightning_title, R.string.lightning_dangerously_close, checkDescendants = true)

        // Run it again and verify the last distance shows at the bottom of the screen
        TestUtils.click(R.id.start_btn)
        TestUtils.click(R.id.start_btn)

        TestUtils.hasText(R.id.previous_strike) {
            val regex = Regex("Last strike at [0-9]+:[0-9]+ [AP]M - $previousDistance")
            regex.matches(it)
        }
    }
}