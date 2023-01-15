package com.kylecorry.trail_sense.tools.maps.ui.mappers

import android.content.Context
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.ceres.list.ListMenuItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup

class MapGroupMapper(
    private val context: Context,
    private val actionHandler: (MapGroup, MapGroupAction) -> Unit
) :
    ListItemMapper<MapGroup> {
    override fun map(value: MapGroup): ListItem {
        return ListItem(
            -value.id,
            value.name,
            context.resources.getQuantityString(
                R.plurals.map_group_summary,
                value.count ?: 0,
                value.count ?: 0
            ),
            icon = ResourceListIcon(R.drawable.ic_beacon_group, size = 48f, foregroundSize = 24f),
            menu = listOf(
                ListMenuItem(context.getString(R.string.rename)) {
                    actionHandler(value, MapGroupAction.Rename)
                },
                ListMenuItem(context.getString(R.string.delete)) {
                    actionHandler(value, MapGroupAction.Delete)
                },
            )
        ) {
            actionHandler(value, MapGroupAction.View)
        }
    }
}