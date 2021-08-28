package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import com.kylecorry.trailsensecore.domain.navigation.INavigationService

class PathListItem(
    private val context: Context,
    private val formatService: FormatService,
    private val prefs: UserPreferences,
    private val navigationService: INavigationService,
    private val delete: (path: List<PathPoint>) -> Unit,
    private val merge: (path: List<PathPoint>) -> Unit,
    private val show: (path: List<PathPoint>) -> Unit
) {

    fun display(
        itemBinding: ListItemPlainIconMenuBinding,
        item: List<PathPoint>
    ) {
        itemBinding.icon.setImageResource(R.drawable.ic_tool_backtrack)
        CustomUiUtils.setImageColor(
            itemBinding.icon,
            Resources.androidTextColorSecondary(context)
        )

        val start = item.first().time
        val end = item.last().time
        val distance = navigationService.getPathDistance(item.map { it.coordinate })
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        itemBinding.title.text = if (start != null && end != null) {
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        } else {
            context.getString(android.R.string.untitled)
        }
        itemBinding.description.text = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
        itemBinding.root.setOnClickListener {
            show(item)
        }
        itemBinding.menuBtn.setOnClickListener {
            Pickers.menu(it, R.menu.path_item_menu) {
                when (it) {
                    R.id.action_path_delete -> {
                        delete(item)
                    }
                    R.id.action_path_merge -> {
                        merge(item)
                    }
                }
                true
            }
        }
    }
}