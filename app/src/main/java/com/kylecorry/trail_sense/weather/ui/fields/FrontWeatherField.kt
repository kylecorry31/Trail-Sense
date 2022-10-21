package com.kylecorry.trail_sense.weather.ui.fields

import android.content.Context
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.science.meteorology.WeatherFront
import com.kylecorry.trail_sense.R

class FrontWeatherField(private val front: WeatherFront?) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        front ?: return null

        val frontName: String
        val icon: Int
        when (front) {
            WeatherFront.Warm -> {
                frontName = context.getString(R.string.weather_warm_front)
                icon = R.drawable.ic_warm_weather_front
            }
            WeatherFront.Cold -> {
                frontName = context.getString(R.string.weather_cold_front)
                icon = R.drawable.ic_cold_weather_front
            }
        }

        return ListItem(
            4,
            context.getString(R.string.weather_front),
            icon = ResourceListIcon(icon),
            trailingText = frontName
        )
    }
}