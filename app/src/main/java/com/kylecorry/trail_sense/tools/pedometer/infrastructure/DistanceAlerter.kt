package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.alerts.RespectfulAlarmAlerter
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.toRelativeDistance

class DistanceAlerter(private val context: Context) : IAlerter {

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val formatter = AppServiceRegistry.get<FormatService>()

    override fun alert() {
        val openIntent = NavigationUtils.pendingIntent(context, R.id.fragmentToolPedometer)

        val distance =
            prefs.pedometer.alertDistance?.convertTo(prefs.baseDistanceUnits)?.toRelativeDistance()

        val notification = Notify.alert(
            context,
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.distance_alert),
            distance?.let {
                context.getString(
                    R.string.distance_alert_distance_reached,
                    formatter.formatDistance(
                        it,
                        Units.getDecimalPlaces(it.units),
                        false
                    )
                )
            },
            R.drawable.steps,
            intent = openIntent,
            autoCancel = true
        )

        Notify.send(context, NOTIFICATION_ID, notification)

        val alarm = RespectfulAlarmAlerter(context, prefs.pedometer.useAlarmForDistanceAlert)
        alarm.alert()
    }

    companion object {
        const val NOTIFICATION_ID = 279852232
        const val NOTIFICATION_CHANNEL_ID = "Distance Alert"
    }

}