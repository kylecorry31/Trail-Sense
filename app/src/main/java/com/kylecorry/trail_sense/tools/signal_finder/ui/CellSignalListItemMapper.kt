package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils

class CellSignalListItemMapper(private val context: Context) : ListItemMapper<CellSignal> {

    private val formatter = AppServiceRegistry.get<FormatService>()

    override fun map(value: CellSignal): ListItem {
        return ListItem(
            value.id.hashCode().toLong(),
            formatter.formatCellNetwork(value.network),
            formatter.join(
                formatter.formatPercentage(value.strength),
                formatter.formatTime(value.time),
                if (value.isRegistered) {
                    context.getString(R.string.full_service)
                } else {
                    context.getString(R.string.emergency_calls_only)
                },
                separator = FormatService.Separator.Dot
            ),
            icon = ResourceListIcon(
                CellSignalUtils.getCellQualityImage(value.quality),
                CustomUiUtils.getQualityColor(value.quality)
            )
        )
    }
}