package com.kylecorry.trail_sense.tools.astronomy

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolAstronomyTest : ToolTestBase(Tools.ASTRONOMY) {

    @Test
    fun verifyBasicFunctionality() {
        // Verify the title
        hasText(R.id.astronomy_title) {
            val valid = listOf(
                string(R.string.until_sunset),
                string(R.string.until_sunrise)
            )
            valid.contains(it)
        }

        hasText(R.id.astronomy_title, Regex("([0-9]+h)? ?([0-9]+m)?"))

        // Verify that today is selected
        hasText(R.id.display_date, string(R.string.today))

        // Verify the list of astronomy events is displayed
        hasText(R.id.astronomy_detail_list) {
            it.startsWith(string(R.string.sun))
        }

        hasText(R.id.astronomy_detail_list) {
            it.startsWith(string(R.string.moon))
        }

        verifyQuickActions()

        // Verify the View in 3D button is visible and works
        if (Tools.isToolAvailable(TestUtils.context, Tools.AUGMENTED_REALITY)) {
            // Wait for the toast do disappear
            not {
                hasText(
                    string(R.string.sunset_alerts_background_location_disclaimer),
                    waitForTime = 0
                )
            }
            click(R.id.button_3d)
            isTrue {
                Tools.getTool(TestUtils.context, Tools.AUGMENTED_REALITY)
                    ?.isOpen(navController.currentDestination?.id ?: 0) == true
            }
        } else {
            isNotVisible(R.id.button_3d)
        }
    }

    @Test
    fun verifyNightMode() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_NIGHT_MODE))

        waitFor {
            scenario.onActivity {
                assertTrue(it.isDarkThemeOn())
                // TODO: Verify the color filter is applied
            }
        }

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_NIGHT_MODE))
    }

    private fun verifyQuickActions() {
        // Verify the sunset alert quick action
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SUNSET_ALERT))

        handleExactAlarmsDialog()

        click(quickAction(Tools.QUICK_ACTION_SUNSET_ALERT))


        click(quickAction(Tools.QUICK_ACTION_SUNRISE_ALERT))

        handleExactAlarmsDialog()

        click(quickAction(Tools.QUICK_ACTION_SUNRISE_ALERT))

        // TODO: Simulate time passing to verify the alerts are shown

        TestUtils.closeQuickActions()
    }
}