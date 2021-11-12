package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import android.graphics.Color
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import java.time.Duration
import java.time.Instant

class WaypointListItem(
    private val context: Context,
    private val isSelected: Boolean,
    private val formatService: FormatService,
    private val prefs: UserPreferences,
    private val createBeacon: (waypoint: PathPoint) -> Unit,
    private val delete: (waypoint: PathPoint) -> Unit,
    private val navigate: (waypoint: PathPoint) -> Unit,
    private val view: (waypoint: PathPoint) -> Unit
) {
    fun display(
        itemBinding: ListItemWaypointBinding,
        item: PathPoint
    ) {
        if (item.time != null) {
            val timeAgo = Duration.between(item.time, Instant.now())
            itemBinding.waypointCoordinates.text =
                context.getString(R.string.time_ago, formatService.formatDuration(timeAgo, false))
            val date = item.time.toZonedDateTime()
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

        itemBinding.root.setBackgroundColor(
            if (isSelected) {
                Resources.color(context, R.color.colorPrimary)
            } else {
                Resources.getAndroidColorAttr(context, android.R.attr.colorBackground)
            }
        )

        itemBinding.root.setOnClickListener {
            view(item)
        }
    }
}