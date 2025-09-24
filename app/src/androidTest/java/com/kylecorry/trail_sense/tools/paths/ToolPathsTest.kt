package com.kylecorry.trail_sense.tools.paths

import androidx.test.uiautomator.Direction
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test


class ToolPathsTest : ToolTestBase(Tools.PATHS) {
    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.paths_title, string(R.string.paths))

        canUseBacktrack()
        canRenamePath()
        canViewPathDetails()
        addPathGroup()
        importPath()
        createEmptyPath()
        groupOperations()
        pathOperations()
        searchPath()
        changeSort()
        // TODO: Quick settings tile
        verifyQuickAction()
    }

    private fun addPathGroup() {
        click(R.id.add_btn)
        click(string(R.string.group))
        input(string(R.string.name), "Test Group")
        clickOk()

        hasText("Test Group")
        click("Test Group")
        hasText(string(R.string.no_paths))
        back(false)
    }

    private fun importPath() {
        // Open import and verify the picker opens, then back out
        click(R.id.add_btn)
        click(string(R.string.import_gpx))
        optional { hasText(string(R.string.pick_file)) }
        back(false)
        hasText(R.id.paths_title, string(R.string.paths))
    }

    private fun createEmptyPath() {
        click(R.id.add_btn)
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Empty Path")
        clickOk()
        isVisible(R.id.path_title)
        hasText("Empty Path")
        backUntil { isVisible(R.id.paths_title, waitForTime = 1000) }
    }

    private fun groupOperations() {
        // Ensure there is a second group to move into
        click(R.id.add_btn)
        click(string(R.string.group), exact = true)
        input(string(R.string.name), "Dest Group")
        clickOk()
        hasText("Dest Group")

        // Rename group "Test Group" -> "Test Group 2"
        hasText("Test Group")
        clickListItemMenu(string(R.string.rename), index = 2)
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")

        // Export current group via toolbar menu
        click(toolbarButton(R.id.paths_title, Side.Right))
        click(string(R.string.export))
        clickOk()
        optional { hasText("trail-sense-") }
        backUntil { isVisible(R.id.paths_title) }

        // Move group into Dest Group
        hasText("Test Group 2")
        clickListItemMenu(string(R.string.move_to), index = 2)
        click("Dest Group")
        click(string(R.string.move))
        optional { hasText(string(R.string.moved_to, "Dest Group")) }

        // Delete moved group (open Dest Group first)
        click("Dest Group")
        hasText("Test Group 2")
        clickListItemMenu(string(R.string.delete))
        clickOk()
        not { hasText("Test Group 2", waitForTime = 0) }
        back(false)
    }

    private fun pathOperations() {
        // Create another path
        click(R.id.add_btn)
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Second Path")
        clickOk()
        hasText("Second Path")
        backUntil { isVisible(R.id.paths_title) }

        // Rename
        clickListItemMenu(string(R.string.rename))
        input("Second Path", "Empty Path 2")
        clickOk()
        hasText("Empty Path 2")

        // Hide / Show
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)

        // Export
        clickListItemMenu(string(R.string.export))
        optional { hasText("trail-sense-") }
        backUntil { isVisible(R.id.paths_title) }

        // Merge "Empty Path 2" into "Empty Path"
        clickListItemMenu(string(R.string.merge))
        click("Empty Path", exact = true)
        optional { hasText(string(R.string.merging)) }
        not { hasText("Empty Path 2") }

        // Simplify path
        clickListItemMenu(string(R.string.simplify))
        click("High")
        clickOk()

        // Move Empty Path to Dest Group
        clickListItemMenu(string(R.string.move_to))
        click("Dest Group")
        click(string(R.string.move))

        // Delete path
        clickListItemMenu(string(R.string.delete), index = 1)
        clickOk()
        not { hasText("Empty Path 2", waitForTime = 0) }
    }

    private fun searchPath() {
        input(R.id.searchbox, "Path")
        hasText("Empty Path")
        not { hasText("Dest Group", waitForTime = 0) }
        input(R.id.searchbox, "")
        hasText("Dest Group")
    }

    private fun changeSort() {
        click(toolbarButton(R.id.paths_title, Side.Right))
        click(string(R.string.sort_by, string(R.string.most_recent)))
        click(string(R.string.name))
        clickOk()
        click(toolbarButton(R.id.paths_title, Side.Right))
        hasText(string(R.string.sort_by, string(R.string.name)))
        back(false)
    }

    private fun canViewPathDetails() {
        // Open the path
        click(com.kylecorry.andromeda.views.R.id.title)

        // Wait for the path to open
        hasText(R.id.path_title, "Test Path")

        // Settings
        click(R.id.path_color)
        clickOk()

        click("Dotted")
        hasOptions("Solid", "Dotted", "Arrow", "Dashed", "Square", "Diamond", "Cross")
        click("Solid")
        clickOk()
        hasText("Solid")

        changePointStyle("None", "Cell signal")
        changePointStyle("Cell signal", "Elevation")
        changePointStyle("Elevation", "Time")
        changePointStyle("Time", "Slope")
        changePointStyle("Slope", "None")

        // Stats
        hasText("0m")
        hasText("Duration")

        hasText("1")
        hasText("Points")

        hasText("0 ft")
        hasText("Distance")

        hasText("Easy")
        hasText("Difficulty")

        scrollUntil(R.id.path_scroll) {
            hasText("0 ft")
            hasText("Ascent")
        }

        hasText("0 ft")
        hasText("Descent")

        scrollUntil(R.id.path_scroll) {
            hasText(Regex("\\d+ ft"))
            hasText("Lowest point")
        }

        hasText(Regex("\\d+ ft"))
        hasText("Highest point")

        scrollUntil(R.id.path_scroll) {
            isVisible(R.id.chart)
        }

        // Add a point
        scrollUntil(R.id.path_scroll) {
            click(R.id.add_point_btn)
        }

        not { hasText("Loading", waitForTime = GPS_WAIT_FOR_TIMEOUT) }

        scrollUntil(R.id.path_scroll, direction = Direction.UP) {
            hasText("2")
        }

        // Navigate
        scrollUntil(R.id.path_scroll) {
            click("Navigate")
        }

        clickOk()
        isVisible(R.id.navigation_title)
        hasText("Test Path")
        back()
        back()
        click("Test Path")

        // Path points
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Points")
        hasText("Today")

        // TODO: This isn't working on the emulator
//        clickListItemMenu("Navigate")
//        isVisible(R.id.navigation_title)
//        hasText("Test Path")
//        back()
//
//        click(toolbarButton(R.id.path_title, Side.Right))
//        click("Points")
//        clickListItemMenu("Create beacon")
//        isVisible(R.id.create_beacon_title)
//        hasText("Test Path")
//        back()
//        click("Leave")
//        back()
//        isVisible(R.id.path_title)
//
//        click(toolbarButton(R.id.path_title, Side.Right))
//        click("Points")
//        clickListItemMenu("Delete")
//        clickOk()
//        back()
//
//        hasText("1")
        back()

        // Simplify
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Simplify")
        hasOptions("High", "Moderate", "Low")
        click("High")
        clickOk()

        // Export
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Export")
        hasText("trail-sense-")
        backUntil {
            isVisible(R.id.path_title)
        }

        // Hide / show
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Hide")
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Show")

        // Keep forever
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Keep forever")
        click(toolbarButton(R.id.path_title, Side.Right))
        not { hasText("Keep forever", waitForTime = 0) }

        backUntil { isVisible(R.id.paths_title) }
    }

    private fun changePointStyle(previousStyle: String, newStyle: String) {
        click(previousStyle)
        click(newStyle)
        clickOk()
        hasText(newStyle)
    }

    private fun hasOptions(vararg options: String) {
        for (option in options) {
            scrollUntil {
                hasText(option)
            }
        }
        optional {
            scrollToStart()
        }
    }

    private fun canRenamePath() {
        clickListItemMenu(string(R.string.rename))
        input(string(R.string.name), "Test Path")
        clickOk()
        hasText(com.kylecorry.andromeda.views.R.id.title, "Test Path")
    }

    private fun canUseBacktrack() {
        // Verify it will run every 15 minutes by default
        hasText(R.id.play_bar_title, "Off - 15m")

        // Click the start button
        click(R.id.play_btn)


        waitFor {
            notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy), waitForTime = 0) }
        }

        hasText(R.id.play_bar_title, "On - 15m")

        // Wait for the path to be created
        isVisible(com.kylecorry.andromeda.views.R.id.title, waitForTime = GPS_WAIT_FOR_TIMEOUT)

        // Stop backtrack
        click(R.id.play_btn)

        not { notification(BacktrackAlerter.NOTIFICATION_ID) }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))

        waitFor {
            notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
        }

        // Wait for the path to be created
        isVisible(
            com.kylecorry.andromeda.views.R.id.title,
            index = 1,
            waitForTime = GPS_WAIT_FOR_TIMEOUT
        )

        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))

        not { notification(BacktrackAlerter.NOTIFICATION_ID) }

        TestUtils.closeQuickActions()
    }
}