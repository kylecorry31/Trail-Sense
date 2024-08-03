package com.kylecorry.trail_sense.test_utils.views

import androidx.test.uiautomator.Direction

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

fun TestView.input(text: String): TestView {
    uiObject.text = text
    return this
}

fun TestView.scroll(direction: Direction = Direction.DOWN, percent: Float = 0.5f): TestView {
    uiObject.scroll(direction, percent)
    return this
}

fun TestView.scrollToEnd(direction: Direction = Direction.DOWN): TestView {
    while (uiObject.scroll(direction, 1f)){
        // Do nothing
    }
    return this
}