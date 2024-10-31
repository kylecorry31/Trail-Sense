package com.kylecorry.trail_sense.test_utils.views

import com.kylecorry.trail_sense.test_utils.TestUtils
import org.junit.Assert.assertTrue


fun TestView.hasText(
    text: String,
    ignoreCase: Boolean = false,
    checkDescendants: Boolean = true,
    contains: Boolean = false
): TestView {
    return hasText(checkDescendants, message = "hasText with text $text") {
        val viewText = if (ignoreCase) it.lowercase() else it
        val compareText = if (ignoreCase) text.lowercase() else text
        if (contains) {
            viewText.contains(compareText)
        } else {
            viewText.trim() == compareText.trim()
        }
    }
}

fun TestView.hasText(
    textResId: Int,
    vararg args: Any,
    ignoreCase: Boolean = false,
    checkDescendants: Boolean = true
): TestView {
    return hasText(TestUtils.getString(textResId, *args), ignoreCase, checkDescendants)
}

fun TestView.hasText(regex: Regex, checkDescendants: Boolean = true): TestView {
    return hasText(checkDescendants, message = "hasText with regex $regex") {
        regex.matches(it)
    }
}

fun TestView.hasText(
    checkDescendants: Boolean = true,
    message: String? = null,
    predicate: (text: String) -> Boolean
): TestView {
    if (!checkDescendants) {
        assertTrue(message ?: "hasText", uiObject.text != null && predicate(uiObject.text))
    } else {
        assertTrue(
            message ?: "hasText",
            TestUtils.matchesSelfOrChild(uiObject) {
                it.text != null && predicate(it.text)
            })
    }
    return this
}
