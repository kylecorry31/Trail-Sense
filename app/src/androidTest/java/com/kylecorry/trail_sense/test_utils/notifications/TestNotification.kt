package com.kylecorry.trail_sense.test_utils.notifications

import android.app.NotificationManager
import android.service.notification.StatusBarNotification
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.test_utils.TestUtils.context

class TestNotification(val notification: StatusBarNotification)

fun notification(id: Int): TestNotification {
    val notifications =
        context.getSystemService<NotificationManager>()?.activeNotifications ?: emptyArray()
    return TestNotification(notifications.first { it.id == id })
}