package com.kylecorry.trail_sense.test_utils

import androidx.annotation.StringRes
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.TestView
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import org.junit.Assert.assertTrue

object AutomationLibrary {

    fun hasText(
        id: Int,
        text: String,
        ignoreCase: Boolean = false,
        checkDescendants: Boolean = true,
        contains: Boolean = false,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id).hasText(
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
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT,
        predicate: (String) -> Boolean
    ) {
        waitFor(waitForTime) {
            view(id).hasText(
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
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id).hasText(text, checkDescendants = checkDescendants)
        }
    }

    fun hasText(text: String, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            viewWithText(text)
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

    fun isNotChecked(
        id: Int,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        isChecked(id, isChecked = false, index = index, waitForTime = waitForTime)
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

    fun click(id: Int, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            view(id, index = index).click()
        }
    }

    fun click(view: TestView, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            view.click()
        }
    }

    fun click(text: String, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            viewWithText(text, index = index).click()
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

    fun isNotVisible(id: Int, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            not { view(id) }
        }
    }

    fun isVisible(id: Int, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            view(id)
        }
    }

    fun isTrue(waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT, predicate: () -> Boolean) {
        waitFor(waitForTime) {
            assertTrue(predicate())
        }
    }

    fun childView(id: Int, childId: Int, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT): TestView {
        return waitFor(waitForTime) {
            view(id, childId)
        }
    }

    const val DEFAULT_WAIT_FOR_TIMEOUT = 5000L
}