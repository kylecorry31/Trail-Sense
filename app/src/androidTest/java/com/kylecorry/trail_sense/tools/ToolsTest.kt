package com.kylecorry.trail_sense.tools

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolsTest : ToolTestBase(0L) {

    @Test
    fun openWithDynamicColors() {
        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName != null) {
            return
        }

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
        // TODO: Figure out how to check this on staging builds (not sure if it will be possible)
        if (AutomationLibrary.packageName != null) {
            return
        }

        scenario.close()

        var calledOriginalHandler = false

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            calledOriginalHandler = true
        }

        // Removing the field guide repo will break the DI for the field guide tool
        AppServiceRegistry.services.remove(FieldGuideRepo::class.java.name)
        scenario = TestUtils.startWithTool(Tools.FIELD_GUIDE)

        isTrue { calledOriginalHandler }
        hasText("An error occurred")
        hasText("Field Guide")

        // Settings
        scrollUntil { click(R.id.open_settings) }
        hasText("Units")

        back()
        hasText("An error occurred")

        // View error details
        click(R.id.view_error_details)
        hasText("Build type")
        clickOk()

        // Other buttons
        isVisible(R.id.email_developer)
        isVisible(R.id.copy_error)
        isVisible(R.id.restart_app)

        // Fix the issue and reopen tool
        AppServiceRegistry.register(FieldGuideRepo.getInstance(TestUtils.context))
        click(R.id.reopen_tool)
        hasText("Disclaimer")
    }
}