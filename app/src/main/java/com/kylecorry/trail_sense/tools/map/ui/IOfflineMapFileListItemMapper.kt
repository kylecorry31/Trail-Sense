package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.trail_sense.tools.map.domain.IOfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileGroup

class IOfflineMapFileListItemMapper(
    context: Context,
    fileActionHandler: (OfflineMapFile, OfflineMapFileAction) -> Unit,
    groupActionHandler: (OfflineMapFileGroup, OfflineMapFileGroupAction) -> Unit
) : ListItemMapper<IOfflineMapFile> {

    private val fileMapper = OfflineMapFileListItemMapper(context, fileActionHandler)
    private val groupMapper = OfflineMapFileGroupListItemMapper(context, groupActionHandler)

    override fun map(value: IOfflineMapFile): ListItem {
        return if (value is OfflineMapFile) {
            fileMapper.map(value)
        } else {
            groupMapper.map(value as OfflineMapFileGroup)
        }
    }
}
