package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.weather.domain.CanSendDailyForecast
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPrediction
import com.kylecorry.trail_sense.weather.infrastructure.alerts.DailyWeatherAlerter
import java.time.LocalDateTime

class DailyWeatherAlertCommand(
    private val prefs: IWeatherPreferences,
    private val forecast: WeatherPrediction,
    private val alerter: IValueAlerter<WeatherPrediction>,
    private val timeProvider: () -> LocalDateTime
) : IWeatherAlertCommand {

    override fun execute() {
        if (!prefs.shouldShowDailyWeatherNotification || !prefs.shouldMonitorWeather) {
            return
        }

        val time = timeProvider.invoke()

        val lastSentDate = prefs.dailyWeatherLastSent
        if (time.toLocalDate() == lastSentDate) {
            return
        }

        if (!CanSendDailyForecast(prefs.dailyForecastTime).isSatisfiedBy(time.toLocalTime())) {
            return
        }

        prefs.dailyWeatherLastSent = time.toLocalDate()
        alerter.alert(forecast)
    }

    companion object {
        fun create(context: Context, forecast: WeatherPrediction): DailyWeatherAlertCommand {
            val prefs = UserPreferences(context).weather
            return DailyWeatherAlertCommand(
                prefs,
                forecast,
                DailyWeatherAlerter(context, FormatService.getInstance(context), prefs)
            ) { LocalDateTime.now() }
        }
    }
}