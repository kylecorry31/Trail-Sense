package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.weather.domain.WeatherAlert

class AlertWeatherField(private val alert: WeatherAlert) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        // TODO: Extract to format service
        val title = when (alert) {
            WeatherAlert.Storm -> context.getString(R.string.weather_storm)
            WeatherAlert.Hot -> context.getString(R.string.heat_warning)
            WeatherAlert.Cold -> context.getString(R.string.cold_warning)
        }
        val color = AppColor.Red.color
        val icon = R.drawable.ic_alert

        return ListItem(
            6293 + alert.id,
            title,
            icon = ResourceListIcon(R.drawable.ic_alert, color)
        )
    }
}