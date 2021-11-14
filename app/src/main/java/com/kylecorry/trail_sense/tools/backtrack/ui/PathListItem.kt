package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.paths.LineStyle
import com.kylecorry.trail_sense.shared.paths.Path

class PathListItem(
    private val context: Context,
    private val formatService: FormatService,
    private val prefs: UserPreferences,
    private val action: (path: Path, action: PathAction) -> Unit
) {

    fun display(
        itemBinding: ListItemPlainIconMenuBinding,
        item: Path
    ) {
        itemBinding.icon.setImageResource(
            if (!item.style.visible) {
                R.drawable.ic_not_visible
            } else when (item.style.line) {
                LineStyle.Solid -> R.drawable.path_solid
                LineStyle.Dotted -> R.drawable.path_dotted
                LineStyle.Arrow -> R.drawable.path_arrow
            }
        )

        CustomUiUtils.setImageColor(
            itemBinding.icon,
            item.style.color
        )

        val distance =
            item.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val nameFactory = PathNameFactory(context)
        itemBinding.title.text = nameFactory.getName(item)
        itemBinding.description.text = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        ) + if (item.temporary) {
            " - " + context.getString(R.string.temporary)
        } else ""
        itemBinding.root.setOnClickListener {
            action(item, PathAction.Show)
        }
        itemBinding.menuBtn.setOnClickListener {
            val actions = listOf(
                PathAction.Rename,
                PathAction.Keep,
                PathAction.ToggleVisibility,
                PathAction.Export,
                PathAction.Merge,
                PathAction.Delete,
            )

            Pickers.menu(
                it, listOf(
                    context.getString(R.string.rename),
                    if (item.temporary) context.getString(R.string.keep_forever) else null,
                    if (prefs.navigation.useRadarCompass || prefs.navigation.areMapsEnabled) {
                        if (item.style.visible) context.getString(R.string.hide) else context.getString(
                            R.string.show
                        )
                    } else null,
                    context.getString(R.string.export),
                    context.getString(R.string.merge),
                    context.getString(R.string.delete),
                )
            ) {
                action(item, actions[it])
                true
            }
        }
    }

}