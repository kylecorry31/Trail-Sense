package com.kylecorry.trail_sense.tools

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
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

    @Test
    fun openWithDynamicColors() {
        val prefs = PreferencesSubsystem.getInstance(TestUtils.context).preferences
        prefs.putBoolean(TestUtils.context.getString(R.string.pref_use_dynamic_colors), true)
        prefs.putBoolean(
            TestUtils.context.getString(R.string.pref_use_dynamic_colors_on_compass),
            true
        )

        // Restart the app with Navigation tool (default)
        scenario.close()
        scenario = TestUtils.startWithTool(Tools.NAVIGATION) {
            navController = it.findNavController()
        }

        // Wait for the tool to load
        Thread.sleep(200)

        // Verify navigation tool is open
        isTrue {
            Tools.getTool(TestUtils.context, Tools.NAVIGATION)!!
                .isOpen(navController.currentDestination?.id ?: 0)
        }
    }

    @Test
    fun catchesToolErrors() {
        // Removing the field guide repo will break the DI for the field guide tool
        AppServiceRegistry.services.remove(FieldGuideRepo::class.java.name)

        navController.openTool(Tools.FIELD_GUIDE)

        hasText("An error occurred")
        hasText("Field Guide")

        // Settings
        click("SETTINGS")
        isTrue {
            Tools.getTool(TestUtils.context, Tools.SETTINGS)!!
                .isOpen(navController.currentDestination?.id ?: 0)
        }

        back()
        hasText("An error occurred")

        // View error details
        click("VIEW ERROR DETAILS")
        hasText("Build type")
        clickOk()

        // Other buttons
        hasText("EMAIL DEVELOPER")
        hasText("COPY ERROR")
        hasText("RESTART APP")

        // Fix the issue and reopen tool
        AppServiceRegistry.register(FieldGuideRepo.getInstance(TestUtils.context))
        click("REOPEN TOOL")
        hasText("Disclaimer")
    }
}