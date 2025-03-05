package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.DEFAULT_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor

enum class Side {
    Left,
    Right
}

val TOOLBAR_LEFT_BUTTON_ID: Int = com.kylecorry.andromeda.views.R.id.andromeda_toolbar_left_button
val TOOLBAR_RIGHT_BUTTON_ID: Int =
    com.kylecorry.andromeda.views.R.id.andromeda_toolbar_right_button

fun toolbarButton(@IdRes toolbarId: Int, side: Side): TestView {
    return waitFor(DEFAULT_WAIT_FOR_TIMEOUT) {
        view(
            toolbarId,
            if (side == Side.Left) TOOLBAR_LEFT_BUTTON_ID else TOOLBAR_RIGHT_BUTTON_ID,
            0
        )
    }
}