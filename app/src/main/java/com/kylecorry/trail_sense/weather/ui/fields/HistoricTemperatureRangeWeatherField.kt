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
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

class HistoricTemperatureRangeWeatherField(
    private val low: Temperature?,
    private val high: Temperature?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        low ?: return null
        high ?: return null
        val formatter = FormatService(context)
        val units = UserPreferences(context).temperatureUnits
        val lowValue = formatter.formatTemperature(
            low.convertTo(units)
        )
        val highValue = formatter.formatTemperature(
            high.convertTo(units)
        )

        val color = when {
            low.temperature <= WeatherSubsystem.COLD -> {
                AppColor.Blue.color
            }
            high.temperature >= WeatherSubsystem.HOT -> {
                AppColor.Red.color
            }
            else -> {
                Resources.androidTextColorSecondary(context)
            }
        }

        return ListItem(
            9,
            context.getString(R.string.temperature_high_low),
            subtitle = context.getString(R.string.historic_temperature_years, 60),
            icon = ResourceListIcon(R.drawable.ic_temperature_range, color),
            trailingText = context.getString(R.string.slash_separated_pair, highValue, lowValue)
        ) {
            onClick()
        }
    }
}