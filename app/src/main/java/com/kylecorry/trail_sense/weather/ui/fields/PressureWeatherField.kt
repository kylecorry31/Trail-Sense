package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences

class PressureWeatherField(private val pressure: Pressure?) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        pressure ?: return null
        val formatter = FormatService(context)
        val units = UserPreferences(context).pressureUnits
        val color = Resources.androidTextColorSecondary(context)
        val value = formatter.formatPressure(
            pressure.convertTo(units),
            Units.getDecimalPlaces(units)
        )

        return ListItem(
            1,
            context.getString(R.string.pressure),
            icon = ResourceListIcon(R.drawable.ic_barometer, color),
            trailingText = value
        )
    }
}