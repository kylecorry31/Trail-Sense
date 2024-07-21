package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.find

class TestView(val uiObject: UiObject2)

fun view(
    @IdRes id: Int,
    @IdRes childId: Int? = null,
    waitForDurationMillis: Long = 5000
): TestView {
    val uiObj = TestUtils.waitFor(waitForDurationMillis) {
        val obj = find(By.res(context.packageName, TestUtils.resourceIdToName(id)))
        if (childId == null) {
            return@waitFor requireNotNull(obj)
        }

        return@waitFor requireNotNull(obj!!.children.find {
            it.resourceName == TestUtils.resourceIdToName(
                childId
            )
        })
    }
    return TestView(uiObj)
}