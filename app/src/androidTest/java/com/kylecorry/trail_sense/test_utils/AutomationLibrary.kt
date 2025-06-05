package com.kylecorry.trail_sense.test_utils

import androidx.annotation.StringRes
import androidx.test.uiautomator.Direction
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.TestView
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.getScrollableView
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.longClick
import com.kylecorry.trail_sense.test_utils.views.scroll
import com.kylecorry.trail_sense.test_utils.views.scrollToEnd
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithHint
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

    fun hasText(
        text: String,
        contains: Boolean = false,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewWithText(text, index = index, contains = contains)
        }
    }

    fun hasText(
        regex: Regex,
        index: Int = 0,
        contains: Boolean = false,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        val r = if (contains) {
            Regex(".*${regex.pattern}.*", RegexOption.DOT_MATCHES_ALL)
        } else {
            regex
        }

        waitFor(waitForTime) {
            viewWithText(r.toPattern(), index = index)
        }
    }

    fun any(vararg actions: () -> Unit, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            var exception: Throwable? = null
            for (action in actions) {
                try {
                    action()
                    return@waitFor
                } catch (e: Throwable) {
                    exception = e
                }
            }
            if (exception != null) {
                throw exception
            }
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
        contains: Boolean = false,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewWithText(viewText, contains, index = index).isChecked(isChecked)
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
        contains: Boolean = false,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        isChecked(
            viewText,
            isChecked = false,
            contains = contains,
            index = index,
            waitForTime = waitForTime
        )
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
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id, index = index).click(holdDuration, xPercent, yPercent)
        }
    }

    fun click(
        view: TestView,
        holdDuration: Long? = null,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view.click(holdDuration, xPercent, yPercent)
        }
    }

    fun click(
        viewLookup: () -> TestView,
        holdDuration: Long? = null,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewLookup().click(holdDuration, xPercent, yPercent)
        }
    }

    fun click(
        text: String,
        index: Int = 0,
        holdDuration: Long? = null,
        contains: Boolean = false,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewWithText(text, index = index, contains = contains).click(
                holdDuration,
                xPercent,
                yPercent
            )
        }
    }

    fun click(
        regex: Regex,
        index: Int = 0,
        holdDuration: Long? = null,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        click(
            { viewWithText(regex.toPattern(), index = index) },
            holdDuration,
            xPercent,
            yPercent,
            waitForTime
        )
    }

    fun longClick(
        id: Int,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view(id, index = index).longClick()
        }
    }

    fun longClick(
        text: String,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            viewWithText(text, index = index).longClick()
        }
    }

    fun longClick(
        view: TestView,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            view.longClick()
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
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT,
        closeKeyboardOnCompletion: Boolean = false
    ) {
        waitFor(waitForTime) {
            view(id).input(text, checkDescendants)
        }
        if (closeKeyboardOnCompletion) {
            TestUtils.back(false)
        }
    }

    fun input(
        viewText: String,
        text: String,
        checkDescendants: Boolean = true,
        contains: Boolean = false,
        isHint: Boolean = false,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            if (isHint) {
                viewWithHint(viewText, contains, index = index)
            } else {
                viewWithText(viewText, contains, index = index)
            }.input(text, checkDescendants)
        }
    }

    fun isNotVisible(id: Int, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            TestUtils.not { view(id, index = index) }
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
        scrollToEnd({ view(id, index = index) }, waitForTime)
    }

    fun scrollToEnd(viewLookup: () -> TestView, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            viewLookup().scrollToEnd()
        }
    }

    fun scrollToStart(id: Int, index: Int = 0, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        scrollToStart({ view(id, index = index) }, waitForTime)
    }

    fun scrollToStart(viewLookup: () -> TestView, waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT) {
        waitFor(waitForTime) {
            viewLookup().scrollToEnd(Direction.UP)
        }
    }

    fun scrollUntil(
        id: Int,
        direction: Direction = Direction.DOWN,
        maxScrolls: Int = 10,
        amountPerScroll: Float = 0.5f,
        index: Int = 0,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT,
        action: () -> Unit
    ) {
        scrollUntil(
            { view(id, index = index) },
            direction,
            maxScrolls,
            amountPerScroll,
            waitForTime,
            action
        )
    }

    fun scrollUntil(
        viewLookup: () -> TestView = { getScrollableView() },
        direction: Direction = Direction.DOWN,
        maxScrolls: Int = 10,
        amountPerScroll: Float = 0.5f,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT,
        action: () -> Unit
    ) {
        var scrollsDone = 0
        while (scrollsDone < maxScrolls) {
            try {
                action()
                // Action succeeded, no need to scroll further
                return
            } catch (e: Throwable) {
                // Action failed, try scrolling
                var scrolled = false
                waitFor(waitForTime) {
                    scrolled = viewLookup().scroll(direction, amountPerScroll)
                }
                if (!scrolled) {
                    // Couldn't scroll further
                    break
                }
                scrollsDone++
            }
        }
        // Try action one last time after all scrolling
        action()
    }

    fun hasNotification(
        id: Int,
        title: String? = null,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            val n = notification(id)
            if (title != null) {
                n.hasTitle(title)
            }
        }
    }

    fun doesNotHaveNotification(
        id: Int,
        waitForTime: Long = DEFAULT_WAIT_FOR_TIMEOUT
    ) {
        waitFor(waitForTime) {
            TestUtils.not { notification(id) }
        }
    }

    const val DEFAULT_WAIT_FOR_TIMEOUT = 5000L
}