package com.kylecorry.trail_sense.tools.notes

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test

class ToolNotesTest : ToolTestBase(Tools.NOTES) {

    private fun openWithANote() {
        assumeFalse(isStagingBuild)
        relaunchTool { TestData.addNote("Test note", "This is a test note") }
        hasText(com.kylecorry.andromeda.views.R.id.title, "Test note")
    }

    @Test
    fun canCreateANote() {
        hasText(R.id.notes_title, string(R.string.tool_notes_title))
        hasText(R.id.notes_empty_text, string(R.string.notes_empty_text))

        click(R.id.add_btn)

        input(R.id.title_edit, "Test note")
        input(R.id.content_edit, "This is a test note")

        click(R.id.note_create_btn)

        hasText(com.kylecorry.andromeda.views.R.id.title, "Test note")
        hasText(com.kylecorry.andromeda.views.R.id.description, "This is a test note")
    }

    @Test
    fun canEditANote() {
        openWithANote()

        click(com.kylecorry.andromeda.views.R.id.title)
        hasText(R.id.title_edit, "Test note")
        hasText(R.id.content_edit, "This is a test note")

        input(R.id.title_edit, "Test note 2")
        input(R.id.content_edit, "This is a test note 2")

        click(R.id.note_create_btn)

        hasText(com.kylecorry.andromeda.views.R.id.title, "Test note 2")
        hasText(com.kylecorry.andromeda.views.R.id.description, "This is a test note 2")
    }

    @Test
    fun canDeleteANote() {
        openWithANote()

        clickListItemMenu(string(R.string.delete))
        clickOk()

        hasText(R.id.notes_empty_text, string(R.string.notes_empty_text))
    }

    @Test
    fun canCreateAQRCodeForANote() {
        openWithANote()

        clickListItemMenu(string(R.string.qr_code))
        hasText(R.id.qr_title, "Test note")
        back(false)
    }

    @Test
    fun canCreateANoteFromTheQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_CREATE_NOTE))

        input(R.id.title_edit, "Quick action note")
        input(R.id.content_edit, "This is a quick action note")

        click(R.id.note_create_btn)

        hasText(com.kylecorry.andromeda.views.R.id.title, "Quick action note")
        hasText(com.kylecorry.andromeda.views.R.id.description, "This is a quick action note")
    }
}
