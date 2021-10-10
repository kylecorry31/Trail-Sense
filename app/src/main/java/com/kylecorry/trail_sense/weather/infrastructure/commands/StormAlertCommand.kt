package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences

class StormAlertCommand(private val context: Context, private val forecast: Weather) :
    IWeatherAlertCommand {

    private val cache by lazy { Preferences(context) }
    private val prefs by lazy { UserPreferences(context) }

    override fun execute() {
        val sentAlert = cache.getBoolean(context.getString(R.string.pref_just_sent_alert)) ?: false

        if (forecast == Weather.Storm) {
            val shouldSend = prefs.weather.sendStormAlerts && prefs.weather.shouldMonitorWeather
            if (shouldSend && !sentAlert) {
                val notification = Notify.alert(
                    context,
                    STORM_CHANNEL_ID,
                    context.getString(R.string.notification_storm_alert_title),
                    context.getString(R.string.notification_storm_alert_text),
                    R.drawable.ic_alert,
                    group = NotificationChannels.GROUP_STORM,
                    intent = NavigationUtils.pendingIntent(context, R.id.action_weather)
                )
                Notify.send(context, STORM_ALERT_NOTIFICATION_ID, notification)
                cache.putBoolean(context.getString(R.string.pref_just_sent_alert), true)
            }
        } else {
            Notify.cancel(context, STORM_ALERT_NOTIFICATION_ID)
            cache.putBoolean(context.getString(R.string.pref_just_sent_alert), false)
        }
    }

    companion object {
        private const val STORM_ALERT_NOTIFICATION_ID = 74309823
        const val STORM_CHANNEL_ID = "Alerts"
    }

}