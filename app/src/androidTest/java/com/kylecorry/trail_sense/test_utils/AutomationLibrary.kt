package com.kylecorry.trail_sense.test_utils

import androidx.annotation.StringRes
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.TestView
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.scrollToEnd
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

object AutomationLibrary {

    fun hasText(
        id: Int,
        text: String,
        ignoreCase: Boolean = false,
        checkDescendants: Boolean = true,
        contains: Boolean = false,
        index: Int = 0,
        childId: Int? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id, childId = childId, index = index).hasText(
                text,
                ignoreCase = ignoreCase,
                checkDescendants = checkDescendants,
                contains = contains
            )
        }
    }

    fun hasText(
        id: Int,
        checkDescendants: Boolean = true,
        message: String? = null,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT,
        predicate: (String) -> Boolean
    ) {
        waitFor(waitForTime) {
            view(id, index = index).hasText(
                checkDescendants = checkDescendants,
                message = message,
                predicate = predicate
            )
        }
    }

    fun hasText(
        id: Int,
        text: Regex,
        checkDescendants: Boolean = true,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id, index = index).hasText(text, checkDescendants = checkDescendants)
        }
    }

    fun hasText(text: String, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            viewWithText(text, index = index)
        }
    }

    fun isChecked(
        id: Int,
        isChecked: Boolean = true,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id, index = index).isChecked(isChecked)
        }
    }

    fun isChecked(
        viewText: String,
        isChecked: Boolean = true,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewWithText(viewText, index = index).isChecked(isChecked)
        }
    }

    fun isNotChecked(
        id: Int,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        isChecked(id, isChecked = false, index = index, waitForTime = waitForTime)
    }

    fun isNotChecked(
        viewText: String,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        isChecked(viewText, isChecked = false, index = index, waitForTime = waitForTime)
    }

    fun string(@StringRes id: Int, vararg args: Any): String {
        return TestUtils.getString(id, *args)
    }

    fun optional(block: () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
            // Do nothing
        }
    }

    fun not(waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT, block: () -> Unit) {
        waitFor(waitForTime) {
            TestUtils.not { block() }
        }
    }

    fun click(
        id: Int,
        index: Int = 0,
        holdDuration: Long? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id, index = index).click(holdDuration)
        }
    }

    fun click(
        view: TestView,
        holdDuration: Long? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view.click(holdDuration)
        }
    }

    fun click(
        text: String,
        index: Int = 0,
        holdDuration: Long? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewWithText(text, index = index).click(holdDuration)
        }
    }

    fun clickOk(index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        click(string(android.R.string.ok), index = index, waitForTime = waitForTime)
    }

    fun input(
        view: TestView,
        text: String,
        checkDescendants: Boolean = true,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view.input(text, checkDescendants)
        }
    }

    fun input(
        id: Int,
        text: String,
        checkDescendants: Boolean = true,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id).input(text, checkDescendants)
        }
    }

    fun input(
        viewText: String,
        text: String,
        checkDescendants: Boolean = true,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewWithText(viewText).input(text, checkDescendants)
        }
    }

    fun isNotVisible(id: Int, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            not { view(id, index = index) }
        }
    }

    fun isVisible(id: Int, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            view(id, index = index)
        }
    }

    fun isTrue(waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT, predicate: () -> Boolean) {
        waitFor(waitForTime) {
            assertTrue(predicate())
        }
    }

    fun isFalse(waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT, predicate: () -> Boolean) {
        waitFor(waitForTime) {
            assertFalse(predicate())
        }
    }

    fun scrollToEnd(id: Int, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            view(id, index = index).scrollToEnd()
        }
    }

    fun childView(id: Int, childId: Int, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT): TestView {
        return waitFor(waitForTime) {
            view(id, childId)
        }
    }

    const val DEFAULT_WAIT_FOR_TIMEOUT = 5000L
}