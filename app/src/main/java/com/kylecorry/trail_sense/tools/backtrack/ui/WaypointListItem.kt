package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import java.time.Duration
import java.time.Instant

class WaypointListItem(
    private val context: Context,
    private val formatService: FormatService,
    private val prefs: UserPreferences,
    private val createBeacon: (waypoint: PathPoint) -> Unit,
    private val delete: (waypoint: PathPoint) -> Unit,
    private val navigate: (waypoint: PathPoint) -> Unit,
) {
    fun display(
        itemBinding: ListItemWaypointBinding,
        item: PathPoint
    ) {
        if (item.time != null) {
            val timeAgo = Duration.between(item.time, Instant.now())
            itemBinding.waypointCoordinates.text =
                context.getString(R.string.time_ago, formatService.formatDuration(timeAgo, false))
            val date = item.time!!.toZonedDateTime()
            val time = date.toLocalTime()
            itemBinding.waypointTime.text = context.getString(
                R.string.waypoint_time_format,
                formatService.formatRelativeDate(date.toLocalDate()),
                formatService.formatTime(time, false)
            )
        } else {
            itemBinding.waypointCoordinates.text = ""
            itemBinding.waypointTime.text = context.getString(android.R.string.untitled)
        }

        if (prefs.backtrackSaveCellHistory) {
            itemBinding.signalStrength.setStatusText(
                CellSignalUtils.getCellTypeString(
                    context,
                    CellNetwork.values().firstOrNull() { it.id == item.cellSignal?.network?.id }
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
            itemBinding.signalStrength.visibility = View.VISIBLE
        } else {
            itemBinding.signalStrength.visibility = View.GONE
        }

        itemBinding.waypointMenuBtn.setOnClickListener {
            Pickers.menu(it, R.menu.waypoint_item_menu) {
                when (it) {
                    R.id.action_waypoint_create_beacon -> {
                        createBeacon(item)
                    }
                    R.id.action_waypoint_delete -> {
                        delete(item)
                    }
                }
                true
            }
        }

        itemBinding.root.setOnClickListener {
            navigate(item)
        }
    }
}