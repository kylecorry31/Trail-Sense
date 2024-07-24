package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.device
import com.kylecorry.trail_sense.test_utils.byResId
import java.util.regex.Pattern

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

fun viewWithText(text: String, parentId: Int? = null, index: Int = 0): TestView {
    return view(By.text(text), index)
}

fun viewWithText(text: Pattern, index: Int = 0): TestView {
    return view(By.text(text), index)
}

fun viewWithText(@StringRes text: Int, index: Int = 0): TestView {
    return view(By.text(TestUtils.getString(text)), index)
}

fun view(selector: BySelector, index: Int = 0): TestView {
    return TestView(requireNotNull(device.findObjects(selector).getOrNull(index)))
}
