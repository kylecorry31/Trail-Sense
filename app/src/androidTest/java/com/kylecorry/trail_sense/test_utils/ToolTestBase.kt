package com.kylecorry.trail_sense.test_utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule

@HiltAndroidTest
open class ToolTestBase(private val toolId: Long) {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @get:Rule
    val screenshotRule = ScreenshotFailureRule()

    protected lateinit var scenario: ActivityScenario<MainActivity>
    protected lateinit var navController: NavController

    private var volume: Int = 0

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()
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