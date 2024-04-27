package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

class BackfillHistoricalTemperaturesCommand(
    private val weather: IWeatherSubsystem,
    private val repo: WeatherRepo
) : CoroutineCommand {
    override suspend fun execute() = onDefault {
        val readings = weather.getRawHistory()

        val updated = readings.map {
            val temperature = weather.getTemperature(
                it.time.toZonedDateTime(),
                it.value.location,
                Distance.meters(it.value.altitude)
            )
            it.copy(value = it.value.copy(temperature = temperature.value.temperature))
        }
        repo.addAll(updated)
    }

    companion object {
        fun create(context: Context): BackfillHistoricalTemperaturesCommand {
            return BackfillHistoricalTemperaturesCommand(
                WeatherSubsystem.getInstance(context),
                WeatherRepo.getInstance(context)
            )
        }
    }
}