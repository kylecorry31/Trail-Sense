package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.CanSendDailyForecast
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPrediction
import java.time.LocalDate
import java.time.LocalTime

class DailyWeatherAlertCommand(
    private val context: Context,
    private val forecast: WeatherPrediction
) :
    IWeatherAlertCommand {

    private val prefs by lazy { UserPreferences(context) }
    private val formatter = FormatService(context)

    override fun execute() {
        if (!prefs.weather.shouldShowDailyWeatherNotification || !prefs.weather.shouldMonitorWeather) {
            return
        }

        val lastSentDate = prefs.weather.dailyWeatherLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        if (!CanSendDailyForecast(prefs.weather.dailyForecastTime).isSatisfiedBy(LocalTime.now())) {
            return
        }

        prefs.weather.dailyWeatherLastSent = LocalDate.now()
        val icon = formatter.getWeatherImage(forecast.primaryHourly)
        val description = formatter.formatWeather(forecast.primaryHourly)

        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_weather)

        val notification = Notify.status(
            context,
            DAILY_CHANNEL_ID,
            context.getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.tomorrows_forecast else R.string.todays_forecast),
            description,
            icon,
            showBigIcon = prefs.weather.showColoredNotificationIcon,
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