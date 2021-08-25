package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import java.time.Duration
import java.time.Instant

class WaypointListItemStrategy(
    private val context: Context,
    private val formatService: FormatServiceV2,
    private val prefs: UserPreferences,
    private val createBeacon: (waypoint: WaypointEntity) -> Unit,
    private val delete: (waypoint: WaypointEntity) -> Unit,
    private val navigate: (waypoint: WaypointEntity) -> Unit,
) : BacktrackListItemStrategy {
    override fun display(
        itemBinding: ListItemWaypointBinding,
        item: FragmentBacktrack.BacktrackListItem
    ) {
        if (item !is FragmentBacktrack.WaypointListItem) {
            return
        }

        itemBinding.waypointImage.setImageResource(R.drawable.ic_location)
        CustomUiUtils.setImageColor(itemBinding.waypointImage, Resources.androidTextColorSecondary(context))
        itemBinding.waypointImage.alpha = 0.2f

        val waypoint = item.waypoint
        val timeAgo = Duration.between(waypoint.createdInstant, Instant.now())
        itemBinding.waypointCoordinates.text =
            context.getString(R.string.time_ago, formatService.formatDuration(timeAgo, false))
        val date = waypoint.createdInstant.toZonedDateTime()
        val time = date.toLocalTime()
        itemBinding.waypointTime.text = context.getString(
            R.string.waypoint_time_format,
            formatService.formatRelativeDate(date.toLocalDate()),
            formatService.formatTime(time, false)
        )

        if (prefs.backtrackSaveCellHistory) {
            itemBinding.signalStrength.setStatusText(
                CellSignalUtils.getCellTypeString(
                    context,
                    // TODO: Return the correct cell network type
                    CellNetwork.values().firstOrNull() { it.id == waypoint.cellNetwork?.id }
                )
            )
            itemBinding.signalStrength.setImageResource(
                CellSignalUtils.getCellQualityImage(
                    waypoint.cellQuality
                )
            )
            itemBinding.signalStrength.setForegroundTint(Color.BLACK)
            itemBinding.signalStrength.setBackgroundTint(
                CustomUiUtils.getQualityColor(
                    waypoint.cellQuality
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
                        createBeacon(waypoint)
                    }
                    R.id.action_waypoint_delete -> {
                        delete(waypoint)
                    }
                }
                true
            }
        }

        itemBinding.root.setOnClickListener {
            navigate(waypoint)
        }
    }
}