package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.NavigationUtils

class DistanceAlertSender(private val context: Context) : IDistanceAlertSender {

    override fun send() {
        val openIntent = NavigationUtils.pendingIntent(context, R.id.fragmentToolPedometer)

        val notification = Notify.alert(
            context,
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.distance_alert),
            null,
            R.drawable.steps,
            intent = openIntent,
            autoCancel = true
        )

        Notify.send(context, NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 279852232
        const val NOTIFICATION_CHANNEL_ID = "Distance Alert"
    }

}