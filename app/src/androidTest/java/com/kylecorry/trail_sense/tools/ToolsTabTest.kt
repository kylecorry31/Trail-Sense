package com.kylecorry.trail_sense.tools

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.closeQuickActions
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolsTabTest : ToolTestBase(0L) {

    @Test
    fun canPinTools() {
        unpinTheCoreTools()

        // Pin Flashlight
        longClick(string(R.string.flashlight_title))
        click(string(R.string.pin))

        hasText(string(R.string.flashlight_title), index = 1)

        // Unpin flashlight
        longClick(string(R.string.flashlight_title))
        click(string(R.string.unpin))
    }

    @Test
    fun canSortTools() {
        unpinTheCoreTools()

        hasText(string(R.string.tool_category_signaling))
        hasText(string(R.string.flashlight_title))

        click(R.id.icon, index = 1)
        click(string(R.string.name))
        clickOk()

        hasText(string(R.string.astronomy))
        not { hasText(string(R.string.tool_category_signaling)) }

        click(R.id.icon, index = 1)
        click(string(R.string.category))
        clickOk()

        hasText(string(R.string.tool_category_signaling))
    }

    @Test
    fun canUseTheQuickActions() {
        click(quickAction(Tools.QUICK_ACTION_WHISTLE))

        openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_WHISTLE))
        closeQuickActions()
    }

    @Test
    fun canSearchOpenAndConfigureTools() {
        unpinTheCoreTools()

        input(R.id.tool_searchbox, "Sett")
        hasText(string(R.string.settings))
        input(R.id.tool_searchbox, "")
        hasText(string(R.string.flashlight_title))

        click(string(R.string.flashlight_title))
        isVisible(R.id.screen_flashlight_btn)
        back()
        isVisible(R.id.tools)

        longClick(string(R.string.flashlight_title))
        click(string(R.string.tool_user_guide_title))
        hasText("SOS")
        back(false)

        longClick(string(R.string.flashlight_title))
        click(string(R.string.settings))
        hasText(string(R.string.pref_flashlight_control_screen_with_volume_title))
        back()
        isVisible(R.id.tools)

        click(R.id.settings_btn)
        hasText(string(R.string.general))
        back()
    }

    private fun unpinTheCoreTools() {
        hasText(string(R.string.pinned))

        click(R.id.icon)
        hasText(string(android.R.string.ok))
        click(string(R.string.astronomy))
        if (Tools.isToolAvailable(context, Tools.WEATHER)) {
            click(string(R.string.weather))
        }
        click(string(R.string.navigation))
        click(string(R.string.tool_user_guide_title))
        clickOk()

        not { hasText(string(R.string.astronomy)) }
    }

}
