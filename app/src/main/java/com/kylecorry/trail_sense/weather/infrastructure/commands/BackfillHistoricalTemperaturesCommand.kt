package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.IWeatherSubsystem
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

class BackfillHistoricalTemperaturesCommand(
    private val weather: IWeatherSubsystem,
    private val repo: WeatherRepo,
    private val prefs: IWeatherPreferences
) : CoroutineCommand {
    override suspend fun execute() = onDefault {
        val readings = weather.getRawHistory()

        // TODO: Extract the calibration
        val sensorMin = prefs.minBatteryTemperature
        val sensorMax = prefs.maxBatteryTemperature
        val calibratedMin = prefs.minActualTemperature
        val calibratedMax = prefs.maxActualTemperature

        val updated = readings.map {
            val temperature = weather.getTemperature(
                it.time.toZonedDateTime(),
                it.value.location,
                Distance.meters(it.value.altitude)
            )
            val calibrated = SolMath.map(
                temperature.value.celsius().temperature,
                sensorMin,
                sensorMax,
                calibratedMin,
                calibratedMax
            )
            it.copy(value = it.value.copy(temperature = calibrated))
        }
        repo.addAll(updated)
    }

    companion object {
        fun create(context: Context): BackfillHistoricalTemperaturesCommand {
            return BackfillHistoricalTemperaturesCommand(
                WeatherSubsystem.getInstance(context),
                WeatherRepo.getInstance(context),
                UserPreferences(context).weather
            )
        }
    }
}