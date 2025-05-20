package com.kylecorry.trail_sense.tools.ballistics

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolBallisticsTest : ToolTestBase(Tools.BALLISTICS) {

    @Test
    fun verifyBasicFunctionality() {
        scope()
        energy()
    }

    private fun scope(){
        click("Scope")

        click(R.id.click_amount)
        click("1/4 MOA")
        clickOk()

        input(R.id.distance_to_target, "100")

        input(R.id.miss_distance_x, "1")
        click(R.id.miss_right)

        scrollToEnd(R.id.scroll_view)
        input(R.id.miss_distance_y, "2")
        click(R.id.miss_up)

        hasText(R.id.adjustment_amount, "Left 12 clicks\nDown 24 clicks")
    }

    private fun energy(){
        click("Energy")

        input(R.id.bullet_speed, "750")
        input(R.id.bullet_weight, "7.5")

        hasText(R.id.energy_amount, "9.4 FPE")
    }

}