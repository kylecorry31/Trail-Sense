package com.kylecorry.trail_sense.weather.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.ceres.list.ResourceListIcon

class WeatherListItemMapper(private val context: Context) : ListItemMapper<WeatherListItem> {
    override fun map(value: WeatherListItem): ListItem {
        return ListItem(
            value.id,
            value.title,
            icon = ResourceListIcon(value.icon, tint = Resources.androidTextColorSecondary(context)),
            trailingText = value.value
        ) {
            value.onClick()
        }
    }
}