package com.kylecorry.trail_sense.tools.clouds

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolCloudsTest : ToolTestBase(Tools.CLOUDS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.cloud_list_title, string(R.string.clouds))
        hasText(R.id.cloud_empty_text, string(R.string.no_clouds))

        canScanCloudFromCamera()
        canScanCloudFromFile()
        canManuallyEnterCloud()
        verifyQuickAction()
    }

    private fun canScanCloudFromCamera() {
        click(R.id.add_btn)

        click(string(R.string.camera))

        click(R.id.capture_button)

        hasText(R.id.cloud_title, string(R.string.clouds))

        // Verify a cloud is selected (the top one)
        isChecked(com.kylecorry.andromeda.views.R.id.checkbox)

        click(toolbarButton(R.id.cloud_title, Side.Right))

        // Back on cloud log
        isVisible(R.id.add_btn)
        click(com.kylecorry.andromeda.views.R.id.title)
        clickOk()
        isVisible(com.kylecorry.andromeda.views.R.id.title)

        // Delete all cloud results
        try {
            while (true) {
                clickListItemMenu(string(R.string.delete))
                clickOk()
            }
        } catch (e: Throwable) {
            // Do nothing
        }

        hasText(R.id.cloud_empty_text, string(R.string.no_clouds))
    }

    private fun canScanCloudFromFile() {
        click(R.id.add_btn)
        click(string(R.string.file))

        // The file picker is opened
        isNotVisible(R.id.add_btn)

        waitFor {
            waitFor {
                back()
            }
            isVisible(R.id.cloud_list_title)
        }
    }

    private fun canManuallyEnterCloud() {
        click(view(R.id.add_btn))

        click(string(R.string.manual))

        hasText(R.id.cloud_title, string(R.string.clouds))

        // Select the first cloud type
        click(com.kylecorry.andromeda.views.R.id.checkbox)

        // Verify it is selected
        isChecked(com.kylecorry.andromeda.views.R.id.checkbox)

        click(toolbarButton(R.id.cloud_title, Side.Right))

        // Delete the cloud result
        clickListItemMenu(string(R.string.delete))
        clickOk()

        hasText(R.id.cloud_empty_text, string(R.string.no_clouds))
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SCAN_CLOUD))

        click(R.id.capture_button)

        hasText(R.id.cloud_title, string(R.string.clouds))

        // Verify a cloud is selected (the top one)
        isChecked(com.kylecorry.andromeda.views.R.id.checkbox)

        click(toolbarButton(R.id.cloud_title, Side.Right))

        // Back on cloud log
        isVisible(R.id.add_btn)
        isVisible(com.kylecorry.andromeda.views.R.id.title)
    }
}