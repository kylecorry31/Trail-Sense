package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.weather.domain.isHigh
import com.kylecorry.trail_sense.weather.domain.isLow

class PressureSystemWeatherField(private val pressure: Pressure?) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        pressure ?: return null
        val name: String
        val description: String
        val icon: Int
        if (pressure.isHigh()) {
            name = context.getString(R.string.high_pressure)
            description = context.getString(R.string.high_pressure_system_description)
            icon = R.drawable.ic_high_pressure_system
        } else if (pressure.isLow()) {
            name = context.getString(R.string.low_pressure)
            description = context.getString(R.string.low_pressure_system_description)
            icon = R.drawable.ic_low_pressure_system
        } else {
            return null
        }

        return ListItem(
            3,
            context.getString(R.string.pressure_system),
            icon = ResourceListIcon(icon),
            trailingText = name
        ) {
            Alerts.dialog(context, name, description, cancelText = null)
        }
    }
}