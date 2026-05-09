package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps

import android.content.Context
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile

class IOfflineMapFileListItemMapper(
    context: Context,
    fileActionHandler: (OfflineMapFile, OfflineMapFileAction) -> Unit,
    groupActionHandler: (MapGroup, OfflineMapFileGroupAction) -> Unit
) : ListItemMapper<IMap> {

    private val fileMapper = OfflineMapFileListItemMapper(context, fileActionHandler)
    private val groupMapper = OfflineMapFileGroupListItemMapper(context, groupActionHandler)

    override fun map(value: IMap): ListItem {
        return if (value is OfflineMapFile) {
            fileMapper.map(value)
        } else {
            groupMapper.map(value as MapGroup)
        }
    }
}
