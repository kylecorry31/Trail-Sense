package com.kylecorry.trail_sense.shared.alerts

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.UserPreferences

class NotificationSubsystem(private val context: Context) {

    enum class GroupBehavior {
        System,
        UngroupAll,
        UngroupHigh
    }

    fun send(notificationId: Int, notification: Notification) {
        val behavior = AppServiceRegistry.get<UserPreferences>().notificationGroupingBehavior

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val shouldUngroup = when (behavior) {
                GroupBehavior.System -> false
                GroupBehavior.UngroupAll -> true
                GroupBehavior.UngroupHigh -> {
                    val channel =
                        Notify.channels(context).firstOrNull { it.id == notification.channelId }
                    channel?.importance == NotificationManager.IMPORTANCE_HIGH
                }
            }

            if (shouldUngroup) {
                val summary = NotificationCompat
                    .Builder(context, notification)
                    .setGroupSummary(true)
                    .setSound(null)
                    .setVibrate(null)
                    .build()
                Notify.send(context, notificationId, summary)
            }
        }
        Notify.send(context, notificationId, notification)
    }

    fun cancel(notificationId: Int) {
        Notify.cancel(context, notificationId)
    }

}