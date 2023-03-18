package com.kylecorry.trail_sense.tools.tides.ui.mappers

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences

class TideListItemMapper(private val context: Context) : ListItemMapper<Tide> {

    private val formatter = FormatService.getInstance(context)
    private val units = UserPreferences(context).baseDistanceUnits

    override fun map(value: Tide): ListItem {
        return ListItem(
            id = value.time.toInstant().toEpochMilli(),
            icon = ResourceListIcon(
                if (value.isHigh) R.drawable.ic_tide_high else R.drawable.ic_tide_low
            ),
            title = if (value.isHigh) context.getString(R.string.high_tide) else context.getString(R.string.low_tide),
            subtitle = if (value.height == null) {
                context.getString(R.string.estimated)
            } else {
                formatter.formatDistance(
                    Distance.meters(value.height!!).convertTo(units), 2, true
                )
            },
            trailingText = formatter.formatTime(value.time.toLocalTime(), includeSeconds = false)
        ) {
            if (value.height == null) {
                Alerts.dialog(
                    context,
                    context.getString(R.string.disclaimer_estimated_tide_title),
                    context.getString(R.string.disclaimer_estimated_tide),
                    cancelText = null
                )
            }
        }
    }
}