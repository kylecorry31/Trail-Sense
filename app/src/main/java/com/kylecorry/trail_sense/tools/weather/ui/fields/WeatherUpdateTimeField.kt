package com.kylecorry.trail_sense.tools.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Instant

class WeatherUpdateTimeField(private val time: Instant?) : WeatherField {
    override fun getListItem(context: Context): ListItem? {
        if (time == null) return null

        val formatService = FormatService.getInstance(context)
        val formattedTime = formatService.formatDateTime(time)

        return ListItem(
            id = 4012L, // A unique ID for the list item (often issue numbers are used, or random Longs)
            title = context.getString(R.string.weather_last_updated), // We will create this next
            subtitle = formattedTime,
            icon = ResourceListIcon(R.drawable.ic_clock) // Use an appropriate icon, check R.drawable for clock/time icons
        )
    }
}
