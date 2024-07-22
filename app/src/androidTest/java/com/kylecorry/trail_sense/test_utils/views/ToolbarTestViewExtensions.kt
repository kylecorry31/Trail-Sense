package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes

enum class Side {
    Left,
    Right
}

fun toolbarButton(@IdRes toolbarId: Int, side: Side): TestView {
    return view(
        toolbarId,
        if (side == Side.Left) com.kylecorry.andromeda.views.R.id.andromeda_toolbar_left_button else com.kylecorry.andromeda.views.R.id.andromeda_toolbar_right_button
    )
}