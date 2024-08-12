package com.kylecorry.trail_sense.tools.augmented_reality

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
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
class ToolAugmentedRealityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.listenForCameraUsage()
        scenario = TestUtils.startWithTool(Tools.AUGMENTED_REALITY)
    }

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.AUGMENTED_REALITY)) {
            return
        }

        // Click OK on the prompts
        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        if (!Sensors.hasGyroscope(context)) {
            waitFor {
                viewWithText(android.R.string.ok).click()
            }
        }

        waitFor(10000) {
            assertTrue(isCameraInUse(isBackFacing = true))
        }

        canTurnOffCamera()
        canCalibrate()
        canToggleLayers()
    }

    private fun canTurnOffCamera() {
        waitFor {
            view(R.id.camera_toggle).click()
        }

        waitFor {
            assertFalse(isCameraInUse(isBackFacing = true))
        }
    }

    private fun canCalibrate() {
        view(R.id.calibrate_btn).click()

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.confirm_calibration_button).click()
        }
    }

    private fun canToggleLayers() {
        view(R.id.layers_btn).click()
        // Verify the layers panel is visible
        waitFor {
            viewWithText(R.string.beacons)
        }

        // Turn off the beacons layer
        viewWithText(R.string.visible, index = 0).click()

        // Close the layers panel
        toolbarButton(R.id.title, Side.Right).click()
        waitFor {
            not { view(R.id.title) }
        }
    }

    @After
    fun tearDown() {
        TestUtils.stopListeningForCameraUsage()
    }
}