package com.kylecorry.trail_sense.tools.whistle

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test


class ToolWhistleTest : ToolTestBase(Tools.WHISTLE) {

    @Test
    fun verifyBasicFunctionality() {
        // Whistle button (needs to press and hold)
        clickTile(R.id.whistle_btn, holdDuration = 200)

        // SOS button
        clickTile(R.id.whistle_sos_btn)

        isAudioOutputActive()
        isTileChecked(R.id.whistle_sos_btn)

        clickTile(R.id.whistle_sos_btn)

        isTileChecked(R.id.whistle_sos_btn, false)

        // Emergency button
        clickTile(R.id.whistle_emergency_btn)

        isAudioOutputActive()
        isTileChecked(R.id.whistle_emergency_btn)

        clickTile(R.id.whistle_emergency_btn)

        isTileChecked(R.id.whistle_emergency_btn, false)

        verifyQuickAction()
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_WHISTLE), holdDuration = 200)
        TestUtils.closeQuickActions()
    }

    private fun clickTile(id: Int, holdDuration: Long? = null) {
        click(id, holdDuration = holdDuration, childId = R.id.tile_btn)
    }

    private fun isAudioOutputActive() {
        isTrue {
            TestUtils.isAudioOutputActive()
        }
    }

    private fun isTileChecked(id: Int, isChecked: Boolean = true) {
        TestUtils.waitFor {
            view(id, childId = R.id.tile_btn).isChecked(isChecked)
        }
    }
}
