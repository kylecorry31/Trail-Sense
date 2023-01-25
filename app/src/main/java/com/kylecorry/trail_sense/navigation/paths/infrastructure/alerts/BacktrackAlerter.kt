package com.kylecorry.trail_sense.navigation.paths.infrastructure.alerts

import android.app.Notification
import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.receivers.StopBacktrackReceiver
import com.kylecorry.trail_sense.navigation.paths.infrastructure.services.BacktrackAlwaysOnService
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter

class BacktrackAlerter(private val context: Context) : IValueAlerter<Distance> {

    private val prefs by lazy { UserPreferences(context) }

    override fun alert(value: Distance) {
        val notification =
            getNotification(context, value.convertTo(prefs.baseDistanceUnits).toRelativeDistance())
        Notify.send(context, NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 578879

        fun getNotification(context: Context, distance: Distance): Notification {
            val formatter = FormatService.getInstance(context)
            val openAction = NavigationUtils.pendingIntent(context, R.id.fragmentBacktrack)

            val stopAction = Notify.action(
                context.getString(R.string.stop),
                StopBacktrackReceiver.pendingIntent(context),
                R.drawable.ic_cancel
            )

            return Notify.persistent(
                context,
                BacktrackAlwaysOnService.FOREGROUND_CHANNEL_ID,
                context.getString(R.string.backtrack),
                formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false),
                R.drawable.ic_tool_backtrack,
                intent = openAction,
                actions = listOf(stopAction),
                showForegroundImmediate = true
            )
        }

    }

}