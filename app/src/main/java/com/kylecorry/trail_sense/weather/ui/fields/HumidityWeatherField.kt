package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor

// TODO: Build chart into this
class HumidityWeatherField(
    private val humidity: Float?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        humidity ?: return null
        if (!Sensors.hasHygrometer(context)) {
            return null
        }

        val formatter = FormatService(context)
        val value = formatter.formatPercentage(humidity)

        return ListItem(
            6,
            context.getString(R.string.humidity),
            icon = ResourceListIcon(R.drawable.ic_category_water, AppColor.Blue.color),
            trailingText = value
        ) {
            onClick()
        }
    }
}