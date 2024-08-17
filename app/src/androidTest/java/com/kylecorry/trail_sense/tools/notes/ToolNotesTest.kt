package com.kylecorry.trail_sense.tools.notes

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
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
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolNotesTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.NOTES)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.notes_title).hasText(R.string.tool_notes_title)
            view(R.id.notes_empty_text).hasText(R.string.notes_empty_text)
        }

        canCreateNote()
        canCreateQRCode()
        canEditNote()
        canDeleteNote()
        verifyQuickAction()
    }

    private fun canEditNote(){
        view(com.kylecorry.andromeda.views.R.id.title).click()
        waitFor {
            view(R.id.title_edit).hasText { it.contains("Test note") }
            view(R.id.content_edit).hasText { it.contains("This is a test note") }
        }

        view(R.id.title_edit).input("Test note 2")
        view(R.id.content_edit).input("This is a test note 2")

        view(R.id.note_create_btn).click()

        waitFor {
            view(com.kylecorry.andromeda.views.R.id.title).hasText("Test note 2")
            view(com.kylecorry.andromeda.views.R.id.description).hasText("This is a test note 2")
        }
    }

    private fun canDeleteNote(){
        clickListItemMenu(getString(R.string.delete))
        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.notes_empty_text).hasText(R.string.notes_empty_text)
        }
    }

    private fun canCreateQRCode(){
        clickListItemMenu(getString(R.string.qr_code))
        waitFor {
            view(R.id.qr_title).hasText("Test note")
        }
        back(false)
    }

    private fun canCreateNote() {
        view(R.id.add_btn).click()

        waitFor {
            view(R.id.title_edit)
        }

        view(R.id.title_edit).input("Test note")
        view(R.id.content_edit).input("This is a test note")

        view(R.id.note_create_btn).click()

        waitFor {
            view(com.kylecorry.andromeda.views.R.id.title).hasText("Test note")
            view(com.kylecorry.andromeda.views.R.id.description).hasText("This is a test note")
        }
    }

    private fun verifyQuickAction(){
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_CREATE_NOTE).click()

        waitFor {
            view(R.id.title_edit)
        }

        view(R.id.title_edit).input("Quick action note")
        view(R.id.content_edit).input("This is a quick action note")

        view(R.id.note_create_btn).click()
        
        waitFor {
            view(com.kylecorry.andromeda.views.R.id.title).hasText("Quick action note")
            view(com.kylecorry.andromeda.views.R.id.description).hasText("This is a quick action note")
        }
    }
}