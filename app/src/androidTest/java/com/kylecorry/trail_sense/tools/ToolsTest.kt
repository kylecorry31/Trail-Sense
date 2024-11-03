package com.kylecorry.trail_sense.tools

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolsTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private lateinit var navController: NavController
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()
        scenario = TestUtils.startWithTool(0L)
        scenario.onActivity {
            navController = it.findNavController()
        }
    }

    @Test
    fun openAllTools() {
        val tools = Tools.getTools(TestUtils.context)
        for (tool in tools) {
            TestUtils.onMain {
                navController.navigate(tool.navAction)
            }
            // Wait for the tool to load
            Thread.sleep(200)
            isTrue {
                tool.isOpen(navController.currentDestination?.id ?: 0)
            }
        }
    }
}