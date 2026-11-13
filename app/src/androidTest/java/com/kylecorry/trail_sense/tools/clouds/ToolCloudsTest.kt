package com.kylecorry.trail_sense.tools.clouds

import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ToolCloudsTest : ToolTestBase(Tools.CLOUDS) {

    @Test
    fun canScanCloudFromCamera() {
        assumeTrue(Camera.hasBackCamera(context))

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

    @Test
    fun canScanCloudFromFileAndManuallyEnterCloud() {
        hasText(R.id.cloud_list_title, string(R.string.clouds))
        hasText(R.id.cloud_empty_text, string(R.string.no_clouds))

        click(R.id.add_btn)
        click(string(R.string.file))

        // The file picker is opened
        isNotVisible(R.id.add_btn)

        backUntil {
            isVisible(R.id.cloud_list_title)
        }

        click(R.id.add_btn)

        click(string(R.string.manual))

        hasText(R.id.cloud_title, string(R.string.clouds))

        // Select the first cloud type (Cirrus is the first CloudGenus)
        click(com.kylecorry.andromeda.views.R.id.checkbox)

        // Verify it is selected
        isChecked(com.kylecorry.andromeda.views.R.id.checkbox)

        click(toolbarButton(R.id.cloud_title, Side.Right))

        hasText(com.kylecorry.andromeda.views.R.id.title, string(R.string.cirrus))

        // Delete the cloud result
        clickListItemMenu(string(R.string.delete))
        clickOk()

        hasText(R.id.cloud_empty_text, string(R.string.no_clouds))
    }

    @Test
    fun listsTheCloudsFromTheLast48HoursNewestFirst() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            val now = Instant.now()
            TestData.addCloud(CloudGenus.Stratus, now.minus(Duration.ofHours(5)))
            TestData.addCloud(CloudGenus.Cirrus, now.minus(Duration.ofDays(3)))
            TestData.addCloud(CloudGenus.Cumulus, now.minus(Duration.ofHours(2)))
        }

        hasTextsInOrder(
            com.kylecorry.andromeda.views.R.id.title,
            listOf(string(R.string.cumulus), string(R.string.stratus))
        )

        // Details
        click(string(R.string.cumulus))
        hasText(string(R.string.cumulus_desc))
        clickOk()

        clickListItemMenu(string(R.string.cumulus), string(R.string.delete))
        clickOk()

        hasTextsInOrder(
            com.kylecorry.andromeda.views.R.id.title,
            listOf(string(R.string.stratus))
        )
    }

    @Test
    fun canScanCloudFromTheQuickAction() {
        assumeTrue(Camera.hasBackCamera(context))

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
