package com.kylecorry.trail_sense.tools.weather.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IDismissibleAlerter
import com.kylecorry.trail_sense.shared.alerts.AlarmAlerter
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class StormAlerter(private val context: Context) : IDismissibleAlerter {

    override fun alert() {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        val notification = Notify.alert(
            context,
            STORM_CHANNEL_ID,
            context.getString(R.string.notification_storm_alert_title),
            context.getString(R.string.notification_storm_alert_text),
            R.drawable.ic_alert,
            group = NOTIFICATION_GROUP_STORM,
            intent = NavigationUtils.pendingIntent(context, R.id.action_weather),
            autoCancel = true
        )
        Notify.send(context, STORM_ALERT_NOTIFICATION_ID, notification)

        val alarm = AlarmAlerter(
            context,
            prefs.weather.useAlarmForStormAlert,
            WeatherToolRegistration.NOTIFICATION_CHANNEL_STORM_ALERT
        )
        alarm.alert()
    }

    override fun dismiss() {
        Notify.cancel(context, STORM_ALERT_NOTIFICATION_ID)
    }

    companion object {
        private const val STORM_ALERT_NOTIFICATION_ID = 74309823
        const val STORM_CHANNEL_ID = "Alerts"
        private const val NOTIFICATION_GROUP_STORM = "trail_sense_storm"

    }
}