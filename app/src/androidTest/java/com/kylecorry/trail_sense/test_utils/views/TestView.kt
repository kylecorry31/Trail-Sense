package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.test_utils.TestUtils.device
import com.kylecorry.trail_sense.test_utils.byResId

class TestView(val uiObject: UiObject2)

fun view(
    @IdRes id: Int,
    @IdRes childId: Int? = null,
    index: Int = 0
): TestView {
    val selector = if (childId == null) {
        byResId(id)
    } else {
        byResId(childId).hasAncestor(byResId(id))
    }
    return view(selector, index)
}

fun view(selector: BySelector, index: Int = 0): TestView {
    return TestView(requireNotNull(device.findObjects(selector).getOrNull(index)))
}
