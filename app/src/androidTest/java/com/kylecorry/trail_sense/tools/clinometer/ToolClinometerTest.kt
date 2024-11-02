package com.kylecorry.trail_sense.tools.clinometer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
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
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()
        TestUtils.listenForCameraUsage()
        TestUtils.startWithTool(Tools.CLINOMETER)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Starts with the camera enabled
        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
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
        click(R.id.incline_container)

        // TODO: Verify it is locked

        // Unlock
        click(R.id.incline_container)
    }

    private fun canSwitchToDialMode() {
        click(toolbarButton(R.id.clinometer_title, Side.Left))

        isTrue {
            !isCameraInUse(isBackFacing = true)
        }
    }

    private fun canEstimateAvalancheRisk() {
        hasText(R.id.avalanche_risk, string(R.string.avalanche_risk))
        hasText(R.id.avalanche_risk, Regex("Low|Moderate|High"))
    }

    private fun canMeasureAngleAndSlope() {
        hasText(R.id.clinometer_title, Regex("-?\\d+°"))
        hasText(R.id.clinometer_title, Regex("-?\\d+% slope"))
    }

    private fun doesNotEstimateHeightOrDistanceByDefault() {
        hasText(R.id.estimated_height, string(R.string.distance_unset))
        hasText(R.id.estimated_height, string(R.string.height))
    }

    private fun canEstimateDistance() {
        click(toolbarButton(R.id.clinometer_title, Side.Right))

        click(string(R.string.distance))

        clickOk()

        input(R.id.amount, "5")
        input(R.id.secondary_amount, "5")

        clickOk()

        // Instructions dialog
        isNotVisible(R.id.amount)
        clickOk()

        hasText(R.id.estimated_height, Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"))
        hasText(R.id.estimated_height, "Distance")
    }

    private fun canEstimateHeight() {
        click(toolbarButton(R.id.clinometer_title, Side.Right))

        click(string(R.string.height))

        clickOk()

        input(R.id.amount, "5")

        clickOk()

        // Instructions dialog
        isNotVisible(R.id.amount)
        clickOk()

        hasText(R.id.estimated_height, Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"))
        hasText(R.id.estimated_height, "Height")
    }

    @After
    fun tearDown() {
        TestUtils.stopListeningForCameraUsage()
    }
}