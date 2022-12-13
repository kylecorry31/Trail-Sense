package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

// TODO: Build chart into this
class TemperatureWeatherField(
    private val temperature: Temperature?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        temperature ?: return null
        val formatter = FormatService(context)
        val prefs = UserPreferences(context)
        val units = prefs.temperatureUnits
        val source = prefs.thermometer.source
        val value = formatter.formatTemperature(
            temperature.convertTo(units)
        )

        val color = when {
            temperature.temperature <= WeatherSubsystem.COLD -> {
                AppColor.Blue.color
            }
            temperature.temperature >= WeatherSubsystem.HOT -> {
                AppColor.Red.color
            }
            else -> {
                Resources.androidTextColorSecondary(context)
            }
        }

        return ListItem(
            5,
            context.getString(R.string.temperature),
            subtitle = when (source) {
                ThermometerSource.Historic -> context.getString(R.string.historic)
                ThermometerSource.Sensor -> context.getString(R.string.sensor)
            },
            icon = ResourceListIcon(R.drawable.thermometer, color),
            trailingText = value
        ) {
            onClick()
        }
    }
}