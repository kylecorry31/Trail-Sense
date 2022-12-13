package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.weather.domain.WeatherAlert

class AlertWeatherField(private val alerts: List<WeatherAlert>) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        if (alerts.isEmpty()) {
            return null
        }

        val formatter = FormatService.getInstance(context)
        val title = context.getString(R.string.alerts)
        val description = alerts.joinToString("\n") { formatter.formatWeatherAlert(it) }
        val moreDescription =
            alerts.joinToString("\n\n") { formatter.formatWeatherAlertDescription(it) }

        return ListItem(
            6293,
            title,
            icon = ResourceListIcon(R.drawable.ic_alert, AppColor.Yellow.color),
            trailingText = description
        ) {
            Alerts.dialog(
                context,
                title,
                moreDescription,
                cancelText = null
            )
        }
    }
}