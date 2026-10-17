package com.kylecorry.trail_sense.tools.paths.infrastructure.alerts

import android.app.Notification
import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.infrastructure.receivers.StopBacktrackReceiver
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService

class BacktrackAlerter(private val context: Context) : IValueAlerter<Path> {

    private val prefs by lazy { UserPreferences(context) }

    override fun alert(value: Path) {
        val distance = value.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val notification = getNotification(context, distance, value.id)
        Notify.update(context, NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 578879
        private const val NOTIFICATION_GROUP = "trail_sense_backtrack"

        fun getNotification(context: Context, distance: Distance, pathId: Long? = null): Notification {
            val formatter = FormatService.getInstance(context)
            val openAction = if (pathId == null) {
                NavigationUtils.pendingIntent(context, R.id.fragmentBacktrack)
            } else {
                NavigationUtils.pendingIntent(
                    context,
                    R.id.pathDetailsFragment,
                    Bundle().apply { putLong("path_id", pathId) }
                )
            }

            val stopAction = Notify.action(
                context.getString(R.string.stop),
                StopBacktrackReceiver.pendingIntent(context),
                R.drawable.ic_cancel
            )

            return Notify.persistent(
                context,
                BacktrackService.FOREGROUND_CHANNEL_ID,
                context.getString(R.string.backtrack),
                formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false),
                R.drawable.ic_tool_backtrack,
                group = NOTIFICATION_GROUP,
                intent = openAction,
                actions = listOf(stopAction),
                showForegroundImmediate = true
            )
        }

    }

}
