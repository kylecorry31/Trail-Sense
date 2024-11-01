package com.kylecorry.trail_sense.tools.sensors

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolSensorsTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setupApplication()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.startWithTool(Tools.SENSORS)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.sensor_details_title).hasText(R.string.pref_sensor_details_title)
        }

        // Verify it displays some sensors (Accelerometer and Battery are available on all test devices)
        waitFor {
            viewWithText(R.string.accelerometer)
        }

        waitFor {
            viewWithText(R.string.tool_battery_title)
        }

        // Click the sensor details button
        toolbarButton(R.id.sensor_details_title, Side.Right).click()

        waitFor {
            viewWithText(R.string.sensors)
            viewWithText("android.sensor.accelerometer (1)")
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.sensor_details_title)
        }
    }
}