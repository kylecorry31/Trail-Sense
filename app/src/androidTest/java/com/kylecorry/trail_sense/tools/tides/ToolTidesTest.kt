package com.kylecorry.trail_sense.tools.tides

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolTidesTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()
        scenario = TestUtils.startWithTool(Tools.TIDES)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Wait for the disclaimer
        clickOk()

        // Wait for the no tides dialog
        hasText(string(R.string.no_tides))
        clickOk()

        // Tide list
        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText(R.id.tides_empty_text, string(R.string.no_tides))

        canCreateTide()
        canViewTide()
        canOpenTideList()
        canEditTide()
    }

    private fun canEditTide() {
        clickListItemMenu(string(R.string.edit), 0)

        hasText(R.id.create_tide_title, string(R.string.tide_table))

        // Verify the fields are set
        hasText(R.id.tide_name) { it.startsWith("Tide 1") }

        // Select lunitidal interval
        click(R.id.estimate_algorithm_spinner)
        click(string(R.string.lunitidal_interval_auto))
        clickOk()

        // Verify the fields are set
        hasText(
            R.id.estimate_algorithm_spinner,
            string(R.string.lunitidal_interval_auto),
            contains = true
        )

        // TODO: Manual lunitidal interval

        // Save
        click(toolbarButton(R.id.create_tide_title, Side.Right))

        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText(com.kylecorry.andromeda.views.R.id.title, "Tide 1")

        // Open the tide
        click(com.kylecorry.andromeda.views.R.id.title)

        // Verify it is now using the lunitidal interval (just check times are shown)
        hasText(R.id.tide_title, string(R.string.high_tide))
    }

    private fun canCreateTide() {
        // Click the add button
        click(R.id.add_btn)

        // Enter the tide details
        hasText(R.id.create_tide_title, string(R.string.tide_table))
        input(R.id.tide_name, "Tide 1")
        isChecked(R.id.tide_frequency_semidiurnal)
        input(R.id.utm, "42, -72")

        click(R.id.estimate_algorithm_spinner)
        isChecked(string(R.string.tide_clock))
        hasText(string(R.string.lunitidal_interval_auto))
        clickOk()

        scrollToEnd(R.id.scroll_view)

        hasText(R.id.tide_type, string(R.string.high_tide_letter))
        click(R.id.tide_type)
        hasText(R.id.tide_type, string(R.string.low_tide_letter))
        click(R.id.tide_type)
        hasText(R.id.tide_type, string(R.string.high_tide_letter))

        click(R.id.tide_time, index = 1)
        clickOk()
        clickOk()

        // TODO: Verify the time is set
        hasText(R.id.tide_time, index = 1) { it.isNotBlank() }

        click(R.id.tide_height, index = 1)
        input(string(R.string.distance), "1.0")
        clickOk()

        hasText(R.id.tide_height, "1.00 ft", index = 1)

        click(R.id.add_tide_entry)

        hasText(R.id.tide_type, string(R.string.high_tide_letter), index = 1)
        click(R.id.delete, index = 1)

        isNotVisible(R.id.tide_type, index = 1)

        click(toolbarButton(R.id.create_tide_title, Side.Right))

        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText(com.kylecorry.andromeda.views.R.id.title, "Tide 1")
        hasText(com.kylecorry.andromeda.views.R.id.description, "1 tide")
    }

    private fun canViewTide() {
        click(com.kylecorry.andromeda.views.R.id.title)
        hasText(R.id.tide_title, string(R.string.high_tide))
        hasText(R.id.tide_title, "Tide 1")

        // Verify that today is selected
        hasText(R.id.tide_list_date, string(R.string.today))

        // Verify at least one high and low tide is shown
        var isHighFirst = false
        hasText(com.kylecorry.andromeda.views.R.id.title) {
            isHighFirst = it == string(R.string.high_tide)
            it == string(R.string.high_tide) || it == string(R.string.low_tide)
        }

        hasText(
            com.kylecorry.andromeda.views.R.id.title, string(
                if (isHighFirst) {
                    R.string.low_tide
                } else {
                    R.string.high_tide
                }
            ), index = 1
        )

        // TODO: Verify the times are correct
        // TODO: Verify the chart
    }

    private fun canOpenTideList() {
        click(toolbarButton(R.id.tide_title, Side.Right))

        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText(com.kylecorry.andromeda.views.R.id.title, "Tide 1")
    }
}