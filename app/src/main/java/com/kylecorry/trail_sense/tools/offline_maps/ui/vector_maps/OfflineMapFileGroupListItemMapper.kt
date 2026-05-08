package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps

import android.content.Context
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.IOfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFileGroup

class OfflineMapFileGroupListItemMapper(
    private val context: Context,
    private val actionHandler: (OfflineMapFileGroup, OfflineMapFileGroupAction) -> Unit
) : ListItemMapper<IOfflineMapFile> {
    override fun map(value: IOfflineMapFile): ListItem {
        val group = value as OfflineMapFileGroup
        return ListItem(
            -group.id,
            group.name,
            context.resources.getQuantityString(
                R.plurals.offline_map_group_summary,
                group.count ?: 0,
                group.count ?: 0
            ),
            icon = ResourceListIcon(
                R.drawable.ic_map_group,
                AppColor.Gray.color,
                size = 48f,
                foregroundSize = 24f
            ),
            menu = listOf(
                ListMenuItem(context.getString(R.string.show_all)) {
                    actionHandler(group, OfflineMapFileGroupAction.ShowAll)
                },
                ListMenuItem(context.getString(R.string.hide_all)) {
                    actionHandler(group, OfflineMapFileGroupAction.HideAll)
                },
                ListMenuItem(context.getString(R.string.rename)) {
                    actionHandler(group, OfflineMapFileGroupAction.Rename)
                },
                ListMenuItem(context.getString(R.string.move_to)) {
                    actionHandler(group, OfflineMapFileGroupAction.Move)
                },
                ListMenuItem(context.getString(R.string.delete)) {
                    actionHandler(group, OfflineMapFileGroupAction.Delete)
                },
            )
        ) {
            actionHandler(group, OfflineMapFileGroupAction.View)
        }
    }
}
