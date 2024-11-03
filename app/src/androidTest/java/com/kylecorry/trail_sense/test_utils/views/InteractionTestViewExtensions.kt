package com.kylecorry.trail_sense.test_utils.views

import androidx.test.uiautomator.Direction
import com.kylecorry.trail_sense.test_utils.TestUtils.getMatchingChild

fun TestView.click(durationMillis: Long? = null): TestView {
    if (durationMillis != null) {
        uiObject.click(durationMillis)
    } else {
        uiObject.click()
    }
    return this
}

fun TestView.longClick(): TestView {
    uiObject.longClick()
    return this
}

fun TestView.input(text: String, checkDescendants: Boolean = false): TestView {
    if (!checkDescendants) {
        uiObject.text = text
    } else {
        val child = getMatchingChild(uiObject) {
            it.className == "android.widget.EditText"
            // TODO: See if this can be exposed
//                    it.getAccessibilityNodeInfo().isEditable
        }

        if (child != null) {
            child.text = text
        }
    }
    return this
}

fun TestView.scroll(direction: Direction = Direction.DOWN, percent: Float = 0.5f): TestView {
    uiObject.scroll(direction, percent)
    return this
}

fun TestView.scrollToEnd(direction: Direction = Direction.DOWN): TestView {
    while (uiObject.scroll(direction, 1f)) {
        // Do nothing
    }
    return this
}