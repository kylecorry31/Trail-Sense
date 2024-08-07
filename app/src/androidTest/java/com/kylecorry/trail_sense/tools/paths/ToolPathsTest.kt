package com.kylecorry.trail_sense.tools.paths

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.getString
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@HiltAndroidTest
class ToolPathsTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.PATHS)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.paths_title).hasText(R.string.paths)
        }

        canUseBacktrack()
        canRenamePath()
        canViewPathDetails()
        // TODO: Add path group
        // TODO: Import path
        // TODO: Create empty path
        // TODO: Rename, export, delete, and move group
        // TODO: Rename, hide/show, export, merge, delete, simplify, and move path
        // TODO: Search path
        // TODO: Change sort
        // TODO: Quick settings tile
        verifyQuickAction()
    }

    private fun canViewPathDetails(){
        // Open the path
        view(com.kylecorry.andromeda.views.R.id.title).click()

        // Wait for the path to open
        waitFor {
            view(R.id.path_title).hasText("Test Path")
        }

        // TODO: Verify the stats are shown
        // TODO: Change path styles
        // TODO: Add a point
        // TODO: Navigate
        // TODO: View path points
        // TODO: Simplify path
        // TODO: Export path
        // TODO: Hide/show path
        back()
    }

    private fun canRenamePath(){
        TestUtils.clickListItemMenu(getString(R.string.rename))
        waitFor {
            viewWithText(R.string.name).input("Test Path")
            viewWithText(android.R.string.ok).click()
        }
        waitFor {
            view(com.kylecorry.andromeda.views.R.id.title).hasText("Test Path")
        }
    }

    private fun canUseBacktrack() {
        // Verify it will run every 15 minutes by default
        view(R.id.play_bar_title).hasText("Off - 15m")

        // Click the start button
        view(R.id.play_btn).click()


        waitFor {
            notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
            view(R.id.play_bar_title).hasText("On - 15m")
        }

        // Wait for the path to be created
        waitFor(12000) {
            view(com.kylecorry.andromeda.views.R.id.title)
        }

        // Stop backtrack
        view(R.id.play_btn).click()

        waitFor {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_BACKTRACK).click()

        waitFor {
            notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
        }

        // Wait for the path to be created
        waitFor(12000) {
            view(com.kylecorry.andromeda.views.R.id.title, index = 1)
        }

        quickAction(Tools.QUICK_ACTION_BACKTRACK).click()

        waitFor {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }

        TestUtils.closeQuickActions()
    }
}