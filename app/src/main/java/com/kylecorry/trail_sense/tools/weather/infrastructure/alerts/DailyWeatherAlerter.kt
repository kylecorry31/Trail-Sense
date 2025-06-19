package com.kylecorry.trail_sense.tools.weather.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences

class DailyWeatherAlerter(
    private val context: Context,
    private val formatter: FormatService,
    private val prefs: IWeatherPreferences
) : IValueAlerter<WeatherPrediction> {
    override fun alert(value: WeatherPrediction) {
        // If daily is unchanging, show hourly instead
        val icon = formatter.getWeatherImage(value.primaryDaily ?: value.primaryHourly)
        val condition = formatter.formatWeather(value.primaryDaily ?: value.primaryHourly)

        val low = value.temperature?.low
        val high = value.temperature?.high

        val temperatureStr = if (high != null && low != null) {
            val units = UserPreferences(context).temperatureUnits
            val lowValue = formatter.formatTemperature(
                low.convertTo(units)
            )
            val highValue = formatter.formatTemperature(
                high.convertTo(units)
            )
            context.getString(R.string.slash_separated_pair, highValue, lowValue)
        } else {
            null
        }

        val description = if (temperatureStr != null) {
            context.getString(R.string.dot_separated_pair, temperatureStr, condition)
        } else {
            condition
        }

        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_weather)

        val notification = Notify.status(
            context,
            DAILY_CHANNEL_ID,
            context.getString(if (prefs.dailyWeatherIsForTomorrow) R.string.tomorrows_forecast else R.string.todays_forecast),
            description,
            icon,
            showBigIcon = prefs.showColoredNotificationIcon,
            group = NOTIFICATION_GROUP_DAILY_WEATHER,
            intent = openIntent,
            autoCancel = true
        )

        AppServiceRegistry.get<NotificationSubsystem>().send(DAILY_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val DAILY_NOTIFICATION_ID = 798643
        const val DAILY_CHANNEL_ID = "daily-weather"
        private const val NOTIFICATION_GROUP_DAILY_WEATHER = "trail_sense_daily_weather"
    }
}