package com.kylecorry.trail_sense.weather.ui.dialogs

import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.weather.ui.charts.TemperatureChart
import java.time.Instant
import java.time.LocalDate

class ShowHighLowTemperatureDialogCommand(
    private val fragment: Fragment,
    private val location: Coordinate? = null,
    private val elevation: Distance? = null
) : CoroutineCommand {

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(fragment.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val temperatureUnits by lazy { UserPreferences(fragment.requireContext()).temperatureUnits }

    override suspend fun execute() {
        val forecast =
            onIO { weatherSubsystem.getTemperatureForecast(LocalDate.now(), location, elevation) }
        val timeOfLow = onDefault {
            forecast.minByOrNull { it.value.temperature }?.time ?: Instant.now()
        }.toZonedDateTime().toLocalTime()
        val timeOfHigh = onDefault {
            forecast.maxByOrNull { it.value.temperature }?.time ?: Instant.now()
        }.toZonedDateTime().toLocalTime()

        val timeStr = MarkdownService(fragment.requireContext()).toMarkdown(
            fragment.getString(
                R.string.high_low_temperature_dialog,
                formatter.formatTime(timeOfLow, includeSeconds = false),
                formatter.formatTime(timeOfHigh, includeSeconds = false),
            )
        )

        val description = buildSpannedString {
            append(timeStr)
            append("\n\n")
            append(fragment.getString(R.string.historical_temperature_disclaimer))
        }

        CustomUiUtils.showChart(
            fragment,
            fragment.getString(R.string.temperature),
            description
        ) {
            val chart = TemperatureChart(it)
            chart.plot(forecast.map { reading ->
                Reading(
                    reading.value.convertTo(temperatureUnits).temperature,
                    reading.time
                )
            })
        }
    }
}