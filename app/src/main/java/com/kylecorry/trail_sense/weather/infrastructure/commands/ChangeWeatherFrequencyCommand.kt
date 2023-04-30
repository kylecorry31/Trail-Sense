package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import java.time.Duration

class ChangeWeatherFrequencyCommand(
    private val context: Context,
    private val onChange: (Duration) -> Unit
) : Command {
    private val prefs by lazy { UserPreferences(context) }
    override fun execute() {
        val title = context.getString(R.string.pref_weather_update_frequency_title)
        CustomUiUtils.pickDuration(
            context,
            prefs.weather.weatherUpdateFrequency,
            title,
            context.getString(R.string.actual_frequency_disclaimer),
            hint = context.getString(R.string.frequency)
        ) {
            if (it != null && !it.isZero) {
                prefs.weather.weatherUpdateFrequency = it
                onChange(it)
                WeatherUpdateScheduler.restart(context)
                if (it < Duration.ofMinutes(15)) {
                    Alerts.dialog(
                        context,
                        context.getString(R.string.battery_warning),
                        context.getString(R.string.backtrack_battery_warning),
                        cancelText = null
                    )
                }

            }
        }
    }
}