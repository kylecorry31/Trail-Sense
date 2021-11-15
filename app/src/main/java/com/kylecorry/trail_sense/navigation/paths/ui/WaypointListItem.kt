package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import android.graphics.Color
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils

class WaypointListItem(
    private val context: Context,
    private val formatService: FormatService,
    private val createBeacon: (waypoint: PathPoint) -> Unit,
    private val delete: (waypoint: PathPoint) -> Unit,
    private val navigate: (waypoint: PathPoint) -> Unit,
    private val view: (waypoint: PathPoint) -> Unit
) {
    fun display(
        itemBinding: ListItemWaypointBinding,
        item: PathPoint
    ) {
        if (item.elevation != null) {
            itemBinding.waypointCoordinates.isVisible = true
            val elevation = Distance.meters(item.elevation)
                .convertTo(UserPreferences(context).baseDistanceUnits)
            itemBinding.waypointCoordinates.text =
                formatService.formatDistance(
                    elevation,
                    Units.getDecimalPlaces(elevation.units),
                    false
                )
        } else {
            itemBinding.waypointCoordinates.isVisible = false
        }
        if (item.time != null) {
            val date = item.time.toZonedDateTime()
            val time = date.toLocalTime()
            itemBinding.waypointTime.text = context.getString(
                R.string.waypoint_time_format,
                formatService.formatRelativeDate(date.toLocalDate()),
                formatService.formatTime(time, false)
            )
        } else {
            itemBinding.waypointTime.text = context.getString(android.R.string.untitled)
        }

        itemBinding.signalStrength.setStatusText(
            formatService.formatCellNetwork(
                CellNetwork.values().firstOrNull { it.id == item.cellSignal?.network?.id }
            )
        )
        itemBinding.signalStrength.setImageResource(
            CellSignalUtils.getCellQualityImage(
                item.cellSignal?.quality ?: Quality.Unknown
            )
        )
        itemBinding.signalStrength.setForegroundTint(Color.BLACK)
        itemBinding.signalStrength.setBackgroundTint(
            CustomUiUtils.getQualityColor(
                item.cellSignal?.quality ?: Quality.Unknown
            )
        )
        itemBinding.signalStrength.isVisible = item.cellSignal != null

        itemBinding.waypointMenuBtn.setOnClickListener {
            Pickers.menu(it, R.menu.waypoint_item_menu) {
                when (it) {
                    R.id.action_waypoint_create_beacon -> {
                        createBeacon(item)
                    }
                    R.id.action_waypoint_delete -> {
                        delete(item)
                    }
                    R.id.action_waypoint_navigate -> {
                        navigate(item)
                    }
                }
                true
            }
        }

        itemBinding.root.setOnClickListener {
            view(item)
        }
    }
}