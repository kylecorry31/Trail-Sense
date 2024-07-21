package com.kylecorry.trail_sense.test_utils.views

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