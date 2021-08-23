package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.shared.DistanceUtils.isLarge
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.navigation.INavigationService
import java.time.Duration

class PathListItemStrategy(
    private val context: Context,
    private val formatService: FormatServiceV2,
    private val prefs: UserPreferences,
    private val navigationService: INavigationService
) : BacktrackListItemStrategy {

    override fun display(
        itemBinding: ListItemWaypointBinding,
        item: FragmentBacktrack.BacktrackListItem
    ) {
        if (item !is FragmentBacktrack.PathListItem) {
            return
        }
        val start = item.path.first().createdInstant
        val end = item.path.last().createdInstant
        val duration = Duration.between(start, end)
        val distance = navigationService.getPathDistance(item.path.map { it.coordinate })
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        itemBinding.signalStrength.isVisible = false
        itemBinding.waypointTime.text =
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        itemBinding.waypointCoordinates.text = context.getString(
            R.string.dot_separated_pair, formatService.formatDistance(
                distance,
                if (distance.units.isLarge()) 2 else 0
            ), formatService.formatDuration(duration)
        )
        itemBinding.waypointImage.isInvisible = true
        itemBinding.root.setOnClickListener(null)
        itemBinding.waypointMenuBtn.setOnClickListener {
            // TODO: Open a menu
        }
    }
}