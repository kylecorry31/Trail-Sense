package com.kylecorry.trail_sense.weather.ui

import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.ceres.list.ResourceListIcon

class WeatherListItemMapper : ListItemMapper<WeatherListItem> {
    override fun map(value: WeatherListItem): ListItem {
        return ListItem(
            value.id,
            value.title,
            icon = ResourceListIcon(value.icon, tint = value.color),
            trailingText = value.value
        ) {
            value.onClick()
        }
    }
}