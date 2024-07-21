package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.device

class TestView(val uiObject: UiObject2)

fun view(
    @IdRes id: Int,
    @IdRes childId: Int? = null,
): TestView {
    val resourceName = context.resources.getResourceEntryName(id)
    val childResourceName = childId?.let { context.resources.getResourceEntryName(it) }
    val obj = device.findObject(By.res(context.packageName, resourceName))
    if (childId == null) {
        return TestView(requireNotNull(obj))
    }

    return TestView(requireNotNull(obj!!.children.find {
        it.resourceName == childResourceName
    }))
}

fun view(selector: BySelector): TestView {
    return TestView(requireNotNull(device.findObject(selector)))
}
