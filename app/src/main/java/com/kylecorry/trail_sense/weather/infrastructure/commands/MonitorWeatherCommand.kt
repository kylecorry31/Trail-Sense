package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.database.IReadingRepo
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.IWeatherSubsystem
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

internal class MonitorWeatherCommand(
    private val repo: IReadingRepo<RawWeatherObservation>,
    private val observer: IWeatherObserver,
    private val subsystem: IWeatherSubsystem,
    private val alerter: IValueAlerter<CurrentWeather>
) : CoroutineCommand {

    override suspend fun execute() {
        sendWeatherNotifications()
        updateWeather()
        sendWeatherNotifications()
    }

    private suspend fun updateWeather() {
        val reading = observer.getWeatherObservation()
        reading?.let { repo.add(it) }
    }

    private suspend fun sendWeatherNotifications() {
        val weather = subsystem.getWeather()
        alerter.alert(weather)
    }

    companion object {
        fun create(context: Context, background: Boolean): MonitorWeatherCommand {
            return MonitorWeatherCommand(
                WeatherRepo.getInstance(context),
                WeatherObserver(context, background, Duration.ofSeconds(10)),
                WeatherSubsystem.getInstance(context),
                WeatherAlerter(context)
            )
        }
    }

}