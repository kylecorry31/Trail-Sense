package com.kylecorry.trail_sense.test_utils

import android.Manifest
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.delay
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils.clearAppData
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.grantPermission
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.After
import org.junit.Before
import org.junit.Rule

open class ToolTestBase(
    private val toolId: Long,
    private val locationOverride: Coordinate? = null
) {

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @get:Rule
    val screenshotRule = ScreenshotFailureRule()

    @get:Rule
    val retryRule = RetryTestRule(maxRetryCount = 3)

    protected lateinit var scenario: ActivityScenario<MainActivity>
    protected lateinit var navController: NavController

    private var volume: Int = 0

    private fun grantBasicPermissions(packageName: String) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.ACTIVITY_RECOGNITION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        for (permission in permissions) {
            grantPermission(packageName, permission)
        }
    }

    @Before
    fun setUp() {
        // TODO: Load from somewhere
//        AutomationLibrary.packageName = "com.kylecorry.trail_sense.staging"
        val packageName = AutomationLibrary.packageName


        TestUtils.setWaitForIdleTimeout()
        if (packageName == null) {
            TestUtils.setupApplication()

            if (locationOverride != null) {
                TestUtils.setLocationOverride(locationOverride)
            }
        }

        TestUtils.listenForCameraUsage()
        TestUtils.listenForTorchUsage()
        volume = TestUtils.mute()
        if (packageName == null) {
            scenario = TestUtils.startWithTool(toolId) {
                navController = it.findNavController()
            }
        } else {
            clearAppData(packageName)
            grantBasicPermissions(packageName)
            TestUtils.launchApp(packageName)
            completeOnboarding()
            click(R.id.action_experimental_tools)
            openTool(toolId)
        }
    }

    private fun completeOnboarding() {
        click("Next")
        click("Next")
        click("Next")
        optional {
            click("Next", waitForTime = 200)
        }
        click("I Agree")
    }

    private fun openTool(id: Long) {
        val tool = Tools.getTool(context, id) ?: return
        input("Search", tool.name)
        scrollUntil { click(tool.name, index = 1) }
        delay(200)
    }

    @After
    fun tearDown() {
        TestUtils.unmute(volume)
        TestUtils.stopListeningForCameraUsage()
        TestUtils.stopListeningForTorchUsage()
        tryOrNothing {
            if (TestUtils.isTorchOn) {
                Torch(context).off()
            }
        }
    }
}