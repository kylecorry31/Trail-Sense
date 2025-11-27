package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils

class CellSignalListItemMapper(private val context: Context) : ListItemMapper<CellSignal> {

    private val formatter = AppServiceRegistry.get<FormatService>()
    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val baseDistanceUnits = prefs.baseDistanceUnits

    override fun map(value: CellSignal): ListItem {
        val distance = value.timingDistanceMeters?.let {
            Distance.meters(it).convertTo(baseDistanceUnits).toRelativeDistance()
        }

        val maxDistance = distance?.let {
            value.timingDistanceErrorMeters?.let {
                val errorDistance = Distance.meters(it).convertTo(distance.units)
                Distance.from(distance.value + errorDistance.value, distance.units)
                    .toRelativeDistance()
            }
        }
        return ListItem(
            value.id.hashCode().toLong(),
            formatter.formatCellNetwork(value.network),
            formatter.join(
                formatter.formatPercentage(value.strength),
                distance?.let {
                    formatter.formatDistance(
                        distance,
                        Units.getDecimalPlaces(distance.units)
                    ) + if (maxDistance != null) {
                        " - ${
                            formatter.formatDistance(
                                maxDistance,
                                Units.getDecimalPlaces(maxDistance.units)
                            )
                        }"
                    } else {
                        ""
                    }
                },
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