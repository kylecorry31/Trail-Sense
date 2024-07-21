package com.kylecorry.trail_sense.test_utils.notifications

import android.app.Notification
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.test_utils.TestUtils
import org.junit.Assert.assertTrue

fun TestNotification.hasTitle(title: String): TestNotification {
    return hasTitle { it == title }
}

fun TestNotification.hasTitle(@IdRes titleId: Int): TestNotification {
    return hasTitle { it == TestUtils.getString(titleId) }
}

fun TestNotification.hasTitle(regex: Regex): TestNotification {
    return hasTitle { regex.matches(it) }
}

fun TestNotification.hasTitle(predicate: (String) -> Boolean): TestNotification {
    assertTrue(
        predicate(
            notification.notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        )
    )
    return this
}