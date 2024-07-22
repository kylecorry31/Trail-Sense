package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.test_utils.byResId

fun quickAction(quickActionId: Int, @IdRes containerRes: Int? = null, index: Int = 0): TestView {
    var selector = By.desc("quick_action_$quickActionId")
    if (containerRes != null) {
        selector = selector.hasAncestor(byResId(containerRes))
    }
    return view(selector, index)
}