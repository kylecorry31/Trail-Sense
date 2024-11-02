package com.kylecorry.trail_sense.tools.clinometer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
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
import kotlin.text.Regex

@HiltAndroidTest
class ToolClinometerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.listenForCameraUsage()
        TestUtils.startWithTool(Tools.CLINOMETER)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Starts with the camera enabled
        waitFor(10000) {
            assertTrue(isCameraInUse(isBackFacing = true))
        }

        canEstimateAvalancheRisk()
        canMeasureAngleAndSlope()
        canLock()
        canSwitchToDialMode()
        doesNotEstimateHeightOrDistanceByDefault()
        canEstimateDistance()
        canEstimateHeight()
    }

    private fun canLock() {
        // Lock
        view(R.id.incline_container).click()

        // TODO: Verify it is locked

        // Unlock
        view(R.id.incline_container).click()
    }

    private fun canSwitchToDialMode() {
        waitFor {
            toolbarButton(R.id.clinometer_title, Side.Left).click()
        }

        waitFor {
            assertFalse(isCameraInUse(isBackFacing = true))
        }
    }

    private fun canEstimateAvalancheRisk() {
        view(R.id.avalanche_risk).hasText(R.string.avalanche_risk)
        view(R.id.avalanche_risk).hasText(Regex("Low|Moderate|High"))
    }

    private fun canMeasureAngleAndSlope() {
        view(R.id.clinometer_title).hasText(Regex("-?\\d+°"))
        view(R.id.clinometer_title).hasText(Regex("-?\\d+% slope"))
    }

    private fun doesNotEstimateHeightOrDistanceByDefault() {
        view(R.id.estimated_height).hasText(R.string.distance_unset)
        view(R.id.estimated_height).hasText(R.string.height)
    }

    private fun canEstimateDistance() {
        toolbarButton(R.id.clinometer_title, Side.Right).click()

        waitFor {
            viewWithText(R.string.distance).click()
        }

        viewWithText(android.R.string.ok).click()

        waitFor {
            view(R.id.amount).input("5")
            view(R.id.secondary_amount).input("5")
        }

        viewWithText(android.R.string.ok).click()

        // Instructions dialog
        waitFor {
            not { view(R.id.amount) }
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.estimated_height).hasText(Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"))
            view(R.id.estimated_height).hasText("Distance")
        }
    }

    private fun canEstimateHeight() {
        toolbarButton(R.id.clinometer_title, Side.Right).click()

        waitFor {
            viewWithText(R.string.height).click()
        }

        viewWithText(android.R.string.ok).click()

        waitFor {
            view(R.id.amount).input("5")
        }

        viewWithText(android.R.string.ok).click()

        // Instructions dialog
        waitFor {
            not { view(R.id.amount) }
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.estimated_height).hasText(Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"))
            view(R.id.estimated_height).hasText("Height")
        }
    }

    @After
    fun tearDown() {
        TestUtils.stopListeningForCameraUsage()
    }
}