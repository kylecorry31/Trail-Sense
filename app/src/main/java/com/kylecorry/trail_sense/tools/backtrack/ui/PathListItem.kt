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
    private val delete: (path: Path2) -> Unit,
    private val merge: (path: Path2) -> Unit,
    private val show: (path: Path2) -> Unit,
    private val export: (path: Path2) -> Unit
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
        val distance = item.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
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
                    R.id.action_path_export -> {
                        export(item)
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