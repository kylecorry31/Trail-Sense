package com.kylecorry.trail_sense.tools.weather.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IDismissibleAlerter
import com.kylecorry.trail_sense.shared.alerts.AlarmAlerter
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class StormAlerter(private val context: Context) : IDismissibleAlerter {

    override fun alert() {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        val useAlarm = prefs.weather.useAlarmForStormAlert
        val notification = Notify.alert(
            context,
            STORM_CHANNEL_ID,
            context.getString(R.string.notification_storm_alert_title),
            context.getString(R.string.notification_storm_alert_text),
            R.drawable.ic_alert,
            group = NOTIFICATION_GROUP_STORM,
            intent = NavigationUtils.pendingIntent(context, R.id.action_weather),
            autoCancel = true,
            mute = useAlarm
        )
        AppServiceRegistry.get<NotificationSubsystem>().send(STORM_ALERT_NOTIFICATION_ID, notification)

        val alarm = AlarmAlerter(
            context,
            useAlarm,
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