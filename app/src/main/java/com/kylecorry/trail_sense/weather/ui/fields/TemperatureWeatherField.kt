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

// TODO: Build chart into this
class TemperatureWeatherField(
    private val temperature: Temperature?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        temperature ?: return null
        val formatter = FormatService(context)
        val units = UserPreferences(context).temperatureUnits
        val value = formatter.formatTemperature(
            temperature.convertTo(units)
        )
        val color: Int
        val icon: Int

        when {
            temperature.temperature <= 15f -> {
                color = AppColor.Blue.color
                icon = R.drawable.ic_thermometer_low
            }
            temperature.temperature >= 25f -> {
                color = AppColor.Red.color
                icon = R.drawable.ic_thermometer_high
            }
            else -> {
                color = Resources.androidTextColorSecondary(context)
                icon = R.drawable.thermometer
            }
        }

        return ListItem(
            5,
            context.getString(R.string.temperature),
            icon = ResourceListIcon(icon, color),
            trailingText = value
        ) {
            onClick()
        }
    }
}