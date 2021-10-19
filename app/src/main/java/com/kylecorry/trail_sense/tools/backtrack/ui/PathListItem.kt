package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.paths.Path2

class PathListItem(
    private val context: Context,
    private val formatService: FormatService,
    private val prefs: UserPreferences,
    private val action: (path: Path2, action: PathAction) -> Unit
) {

    fun display(
        itemBinding: ListItemPlainIconMenuBinding,
        item: Path2
    ) {
        itemBinding.icon.setImageResource(if (item.temporary) R.drawable.ic_update else R.drawable.ic_tool_backtrack)
        CustomUiUtils.setImageColor(
            itemBinding.icon,
            Resources.androidTextColorSecondary(context)
        )

        val start = item.metadata.duration?.start
        val end = item.metadata.duration?.end
        val distance =
            item.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        itemBinding.title.text = if (item.name != null){
            item.name
        } else if (start != null && end != null) {
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
            action(item, PathAction.Show)
        }
        itemBinding.menuBtn.setOnClickListener {
            // TODO: Don't use the menu XML to conditionally display options
            Pickers.menu(it, R.menu.path_item_menu) {
                when (it) {
                    R.id.action_path_delete -> {
                        action(item, PathAction.Delete)
                    }
                    R.id.action_path_export -> {
                        action(item, PathAction.Export)
                    }
                    R.id.action_path_merge -> {
                        action(item, PathAction.Merge)
                    }
                    R.id.action_path_rename -> {
                        action(item, PathAction.Rename)
                    }
                    R.id.action_path_keep -> {
                        action(item, PathAction.Keep)
                    }
                }
                true
            }
        }
    }
}