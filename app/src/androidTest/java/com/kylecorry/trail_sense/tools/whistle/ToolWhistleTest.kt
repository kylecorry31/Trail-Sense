package com.kylecorry.trail_sense.tools.whistle

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickQuickAction
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isFalse
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test


class ToolWhistleTest : ToolTestBase(Tools.WHISTLE) {

    @Test
    fun canPlayTheWhistleAndTheSignals() {
        // Whistle button (needs to press and hold)
        click(R.id.whistle_btn, holdDuration = 200)

        click(R.id.whistle_sos_btn)

        isTrue {
            TestUtils.isPlayingMusic()
        }

        click(R.id.whistle_sos_btn)

        isFalse {
            TestUtils.isPlayingMusic()
        }

        click(R.id.whistle_emergency_btn)

        isTrue {
            TestUtils.isPlayingMusic()
        }

        click(R.id.whistle_emergency_btn)

        isFalse {
            TestUtils.isPlayingMusic()
        }
    }

    @Test
    fun canPlayTheWhistleFromTheQuickAction() {
        TestUtils.openQuickActions()
        clickQuickAction(Tools.QUICK_ACTION_WHISTLE, holdDuration = 200)
        TestUtils.closeQuickActions()
    }
}
