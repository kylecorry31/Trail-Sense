package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.lists.ListItem
import com.kylecorry.trail_sense.shared.lists.ListItemMapper
import com.kylecorry.trail_sense.shared.lists.ListMenuItem
import com.kylecorry.trail_sense.shared.lists.ResourceListIcon

class PathListItemMapper(
    private val context: Context,
    private val actionHandler: (Path, PathAction) -> Unit
) : ListItemMapper<Path> {
    override fun map(value: Path): ListItem {
        return value.toListItem(context) { actionHandler(value, it) }
    }

    private fun Path.toListItem(
        context: Context,
        action: (action: PathAction) -> Unit
    ): ListItem {
        val prefs = UserPreferences(context)
        val formatService = FormatService.getInstance(context)
        val nameFactory = PathNameFactory(context)
        val distance =
            metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val icon = if (!style.visible) {
            R.drawable.ic_not_visible
        } else when (style.line) {
            LineStyle.Solid -> R.drawable.path_solid
            LineStyle.Dotted -> R.drawable.path_dotted
            LineStyle.Arrow -> R.drawable.path_arrow
            LineStyle.Dashed -> R.drawable.path_dashed
            LineStyle.Square -> R.drawable.path_square
            LineStyle.Diamond -> R.drawable.path_diamond
            LineStyle.Cross -> R.drawable.path_cross
        }

        val menu = listOfNotNull(
            ListMenuItem(context.getString(R.string.rename)) { action(PathAction.Rename) },
            if (temporary) ListMenuItem(context.getString(R.string.keep_forever)) {
                action(PathAction.Keep)
            } else null,
            ListMenuItem(
                if (style.visible) context.getString(R.string.hide) else context.getString(
                    R.string.show
                )
            ) {
                action(PathAction.ToggleVisibility)
            },
            ListMenuItem(context.getString(R.string.export)) { action(PathAction.Export) },
            ListMenuItem(context.getString(R.string.merge)) { action(PathAction.Merge) },
            ListMenuItem(context.getString(R.string.delete)) { action(PathAction.Delete) },
            ListMenuItem(context.getString(R.string.simplify)) { action(PathAction.Simplify) }
        )

        val description = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )

        return ListItem(
            id,
            nameFactory.getName(this),
            icon = ResourceListIcon(
                icon,
                style.color
            ),
            description = description,
            subtitle = if (temporary) context.getString(R.string.temporary) else null,
            menu = menu
        ) {
            action(PathAction.Show)
        }
    }
}