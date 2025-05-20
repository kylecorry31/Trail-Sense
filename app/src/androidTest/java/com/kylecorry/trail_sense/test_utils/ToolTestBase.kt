package com.kylecorry.trail_sense.test_utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.test_utils.TestUtils.context
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

    @Before
    fun setUp() {
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()

        if (locationOverride != null) {
            TestUtils.setLocationOverride(locationOverride)
        }

        TestUtils.listenForCameraUsage()
        TestUtils.listenForTorchUsage()
        volume = TestUtils.mute()
        scenario = TestUtils.startWithTool(toolId) {
            navController = it.findNavController()
        }
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