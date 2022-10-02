package com.kylecorry.trail_sense.shared.debugging

import android.content.Context
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.WeatherObservation

class DebugWeatherCommand(
    private val context: Context,
    private val original: List<Reading<RawWeatherObservation>>,
    private val smoothed: List<WeatherObservation>,
    private val factorInTemperature: Boolean
) : DebugCommand() {
    override fun executeDebug() {
        val header = listOf(
            listOf(
                "time",
                "raw_pressure",
                "raw_altitude",
                "raw_sea_level",
                "raw_temperature",
                "raw_humidity",
                "smooth_pressure",
                "smooth_temperature",
                "smooth_humidity"
            )
        )
        val data = header + smoothed.map {
            val originalReading = original.firstOrNull { r -> r.time == it.time }
            listOf(
                it.time.toEpochMilli(),
                originalReading?.value?.pressure,
                originalReading?.value?.altitude,
                originalReading?.value?.seaLevel(useTemperature = factorInTemperature)?.pressure,
                originalReading?.value?.temperature,
                originalReading?.value?.humidity,
                it.pressure.pressure,
                it.temperature.temperature,
                it.humidity ?: 0f
            )
        }

        FileSubsystem.getInstance(context).writeDebug(
            "weather.csv",
            CSVConvert.toCSV(data)
        )
    }
}