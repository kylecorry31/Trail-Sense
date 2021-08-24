package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.isLarge
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trailsensecore.domain.navigation.INavigationService

class PathListItemStrategy(
    private val context: Context,
    private val formatService: FormatServiceV2,
    private val prefs: UserPreferences,
    private val navigationService: INavigationService,
    private val delete: (path: List<WaypointEntity>) -> Unit,
    private val merge: (path: List<WaypointEntity>) -> Unit,
) : BacktrackListItemStrategy {

    override fun display(
        itemBinding: ListItemWaypointBinding,
        item: FragmentBacktrack.BacktrackListItem
    ) {
        if (item !is FragmentBacktrack.PathListItem) {
            return
        }

        itemBinding.waypointImage.setImageResource(R.drawable.ic_tool_backtrack)
        CustomUiUtils.setImageColor(itemBinding.waypointImage, Resources.androidTextColorSecondary(context))//Resources.color(context, R.color.colorPrimary))
        itemBinding.waypointImage.alpha = 1f

        val start = item.path.first().createdInstant
        val end = item.path.last().createdInstant
        val distance = navigationService.getPathDistance(item.path.map { it.coordinate })
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        itemBinding.signalStrength.isVisible = false
        itemBinding.waypointTime.text =
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        itemBinding.waypointCoordinates.text = formatService.formatDistance(
            distance,
            if (distance.units.isLarge()) 2 else 0
        )
        itemBinding.root.setOnClickListener(null)
        itemBinding.waypointMenuBtn.setOnClickListener {
            Pickers.menu(it, R.menu.path_item_menu) {
                when (it) {
                    R.id.action_path_delete -> {
                        delete(item.path)
                    }
                    R.id.action_path_merge -> {
                        merge(item.path)
                    }
                }
                true
            }
        }
    }
}