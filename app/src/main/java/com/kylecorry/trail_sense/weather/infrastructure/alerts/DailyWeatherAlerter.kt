package com.kylecorry.trail_sense.weather.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPrediction

class DailyWeatherAlerter(
    private val context: Context,
    private val formatter: FormatService,
    private val prefs: IWeatherPreferences
) : IValueAlerter<WeatherPrediction> {
    override fun alert(value: WeatherPrediction) {
        // If daily is unchanging, show hourly instead
        val icon = formatter.getWeatherImage(value.primaryDaily ?: value.primaryHourly)
        val description = formatter.formatWeather(value.primaryDaily ?: value.primaryHourly)

        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_weather)

        val notification = Notify.status(
            context,
            DAILY_CHANNEL_ID,
            context.getString(if (prefs.dailyWeatherIsForTomorrow) R.string.tomorrows_forecast else R.string.todays_forecast),
            description,
            icon,
            showBigIcon = prefs.showColoredNotificationIcon,
            group = NotificationChannels.GROUP_DAILY_WEATHER,
            intent = openIntent,
            autoCancel = true
        )

        Notify.send(context, DAILY_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val DAILY_NOTIFICATION_ID = 798643
        const val DAILY_CHANNEL_ID = "daily-weather"
    }
}