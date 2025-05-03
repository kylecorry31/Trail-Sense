package com.kylecorry.trail_sense.test_utils.views

import android.graphics.Point
import androidx.test.uiautomator.Direction
import com.kylecorry.trail_sense.test_utils.TestUtils.getMatchingChild

fun TestView.click(
    durationMillis: Long? = null,
    xPercent: Float? = null,
    yPercent: Float? = null
): TestView {

    val point = if (xPercent != null && yPercent != null) {
        val bounds = uiObject.visibleBounds
        Point(
            bounds.left + (bounds.width() * xPercent).toInt(),
            bounds.top + (bounds.height() * yPercent).toInt()
        )
    } else {
        null
    }

    if (point == null) {
        if (durationMillis != null) {
            uiObject.click(durationMillis)
        } else {
            uiObject.click()
        }
    } else {
        if (durationMillis != null) {
            uiObject.click(point, durationMillis)
        } else {
            uiObject.click(point)
        }
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

fun TestView.scroll(direction: Direction = Direction.DOWN, percent: Float = 0.5f): Boolean {
    val scrolled = uiObject.scroll(direction, percent)
    // Wait after scrolling (Android 16 issue)
    Thread.sleep(500)
    return scrolled
}

fun TestView.scrollToEnd(direction: Direction = Direction.DOWN): TestView {
    while (uiObject.scroll(direction, 1f)) {
        // Do nothing
    }
    // Wait after scrolling (Android 16 issue)
    Thread.sleep(500)
    return this
}