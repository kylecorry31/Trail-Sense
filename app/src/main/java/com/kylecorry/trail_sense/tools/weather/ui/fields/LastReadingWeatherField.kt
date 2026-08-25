package com.kylecorry.trail_sense.tools.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Instant

class LastReadingWeatherField(private val time: Instant?) : WeatherField {

    override fun getListItem(context: Context): ListItem? {
        time ?: return null

        val formatter = FormatService.getInstance(context)
        val color = Resources.androidTextColorSecondary(context)

        return ListItem(
            1,
            context.getString(R.string.last_reading),
            icon = ResourceListIcon(R.drawable.ic_tool_clock, color),
            trailingText = formatter.formatDateTime(
                time.toZonedDateTime(),
            )
        )
    }
}
