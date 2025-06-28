package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path

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
            ListMenuItem(context.getString(R.string.export)) { action(PathAction.Export) },
            ListMenuItem(context.getString(R.string.merge)) { action(PathAction.Merge) },
            ListMenuItem(context.getString(R.string.delete)) { action(PathAction.Delete) },
            ListMenuItem(context.getString(R.string.simplify)) { action(PathAction.Simplify) },
            ListMenuItem(context.getString(R.string.move_to)) { action(PathAction.Move) }
        )

        val distanceString = formatService.formatDistance(
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
            trailingIcon = ResourceListIcon(
                if (style.visible) {
                    R.drawable.ic_visible
                } else {
                    R.drawable.ic_not_visible
                },
                Resources.androidTextColorSecondary(context),
                onClick = {
                    action(PathAction.ToggleVisibility)
                }
            ),
            subtitle = buildSpannedString {
                if (temporary) {
                    bold { append(context.getString(R.string.temporary)) }
                    append("    ")
                }
                append(distanceString)
            },
            menu = menu
        ) {
            action(PathAction.Show)
        }
    }
}