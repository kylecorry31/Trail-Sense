package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.science.meteorology.forecast.Weather
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.CanSendDailyForecast
import java.time.LocalDate
import java.time.LocalTime

class DailyWeatherAlertCommand(private val context: Context, private val forecast: Weather) :
    IWeatherAlertCommand {

    private val prefs by lazy { UserPreferences(context) }

    override fun execute() {
        if (!prefs.weather.shouldShowDailyWeatherNotification) {
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
        val icon = when (forecast) {
            Weather.ImprovingSlow -> R.drawable.sunny
            Weather.WorseningSlow -> R.drawable.light_rain
            else -> R.drawable.steady
        }

        val description = when (forecast) {
            Weather.ImprovingSlow -> context.getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.weather_better_than_today else R.string.weather_better_than_yesterday)
            Weather.WorseningSlow -> context.getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.weather_worse_than_today else R.string.weather_worse_than_yesterday)
            else -> context.getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.weather_same_as_today else R.string.weather_same_as_yesterday)
        }

        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_weather)

        val notification = Notify.status(
            context,
            DAILY_CHANNEL_ID,
            context.getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.tomorrows_forecast else R.string.todays_forecast),
            description,
            icon,
            showBigIcon = prefs.weather.showColoredNotificationIcon,
            group = NotificationChannels.GROUP_DAILY_WEATHER,
            intent = openIntent
        )

        Notify.send(context, DAILY_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val DAILY_NOTIFICATION_ID = 798643
        const val DAILY_CHANNEL_ID = "daily-weather"
    }

}