package com.kylecorry.trail_sense.weather.ui.dialogs

import androidx.fragment.app.Fragment
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.weather.ui.charts.TemperatureChart
import java.time.ZonedDateTime

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
            onIO {
                weatherSubsystem.getTemperatureForecast(
                    ZonedDateTime.now(),
                    ZonedDateTime.now().plusHours(24),
                    location,
                    elevation
                )
            }

        val now = formatter.formatTemperature(forecast.first().value.convertTo(temperatureUnits))

        CustomUiUtils.showChart(
            fragment,
            fragment.getString(R.string.next_24_hours),
            fragment.getString(R.string.historical_temperature_disclaimer) + "\n\n" + fragment.getString(
                R.string.now_value,
                now
            )
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