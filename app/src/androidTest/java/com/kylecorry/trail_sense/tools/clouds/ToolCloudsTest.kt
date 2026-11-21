package com.kylecorry.trail_sense.tools.clouds

import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickQuickAction
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickRightToolbarButton
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.deleteListItem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.ListItem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration
import java.time.Instant
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

class ToolCloudsTest : ToolTestBase(Tools.CLOUDS) {

    @Test
    fun canScanCloudFromCamera() {
        assumeTrue(Camera.hasBackCamera(context))

        click(R.id.add_btn)

        click(string(R.string.camera))

        click(R.id.capture_button)

        hasText(R.id.cloud_title, string(R.string.clouds))

        isChecked(ListItem.checkbox)

        clickRightToolbarButton(R.id.cloud_title)

        isVisible(R.id.add_btn)
        click(ListItem.title)
        clickOk()
        isVisible(ListItem.title)

        // Delete all cloud results
        try {
            while (true) {
                deleteListItem()
            }
        } catch (e: Throwable) {
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
        click(ListItem.checkbox)

        isChecked(ListItem.checkbox)

        clickRightToolbarButton(R.id.cloud_title)

        hasText(ListItem.title, string(R.string.cirrus))

        deleteListItem()

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
            ListItem.title,
            listOf(string(R.string.cumulus), string(R.string.stratus))
        )

        click(string(R.string.cumulus))
        hasText(string(R.string.cumulus_desc))
        clickOk()

        deleteListItem(string(R.string.cumulus))

        hasTextsInOrder(
            ListItem.title,
            listOf(string(R.string.stratus))
        )
    }

    @Test
    fun canScanCloudFromTheQuickAction() {
        assumeTrue(Camera.hasBackCamera(context))

        TestUtils.openQuickActions()
        clickQuickAction(Tools.QUICK_ACTION_SCAN_CLOUD)

        click(R.id.capture_button)

        hasText(R.id.cloud_title, string(R.string.clouds))

        isChecked(ListItem.checkbox)

        clickRightToolbarButton(R.id.cloud_title)

        isVisible(R.id.add_btn)
        isVisible(ListItem.title)
    }
}
