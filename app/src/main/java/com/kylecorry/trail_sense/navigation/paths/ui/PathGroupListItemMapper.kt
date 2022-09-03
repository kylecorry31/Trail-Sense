package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.ceres.list.ListMenuItem
import com.kylecorry.ceres.list.ResourceListIcon

class PathGroupListItemMapper(
    private val context: Context,
    private val actionHandler: (PathGroup, PathGroupAction) -> Unit
) : ListItemMapper<PathGroup> {
    override fun map(value: PathGroup): ListItem {
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
            },
            ListMenuItem(context.getString(R.string.move_to)) {
                actionHandler(
                    value,
                    PathGroupAction.Move
                )
            }
        )

        val count = value.count ?: 0

        return ListItem(
            -value.id,
            value.name,
            icon = ResourceListIcon(
                R.drawable.ic_path_group,
                AppColor.Gray.color
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