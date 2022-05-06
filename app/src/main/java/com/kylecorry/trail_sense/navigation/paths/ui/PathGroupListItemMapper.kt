package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.shared.lists.ListItem
import com.kylecorry.trail_sense.shared.lists.ListItemMapper
import com.kylecorry.trail_sense.shared.lists.ListMenuItem
import com.kylecorry.trail_sense.shared.lists.ResourceListIcon

class PathGroupListItemMapper(
    private val context: Context,
    private val actionHandler: (PathGroup, PathGroupAction) -> Unit
) : ListItemMapper<PathGroup> {
    override fun map(value: PathGroup): ListItem {
        val icon = R.drawable.ic_tool_backtrack

        val menu = listOfNotNull(
            ListMenuItem(context.getString(R.string.rename)) {
                actionHandler(
                    value,
                    PathGroupAction.Rename
                )
            },
            ListMenuItem(context.getString(R.string.delete)) {
                actionHandler(
                    value,
                    PathGroupAction.Delete
                )
            }
        )

        val count = value.count ?: 0

        return ListItem(
            -value.id,
            value.name,
            icon = ResourceListIcon(
                icon,
                Resources.androidTextColorSecondary(context)
            ),
            subtitle = context.resources.getQuantityString(
                R.plurals.path_group_summary,
                count,
                count
            ),
            menu = menu
        ) {
            actionHandler(value, PathGroupAction.Open)
        }
    }

}