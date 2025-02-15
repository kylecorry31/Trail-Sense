package com.kylecorry.trail_sense.tools

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolsTest : ToolTestBase(0L) {

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