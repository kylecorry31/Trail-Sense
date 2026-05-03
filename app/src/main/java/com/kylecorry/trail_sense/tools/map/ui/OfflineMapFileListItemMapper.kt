package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile

class OfflineMapFileListItemMapper(
    private val context: Context,
    private val actionHandler: (OfflineMapFile, OfflineMapFileAction) -> Unit
) : ListItemMapper<OfflineMapFile> {

    private val formatter = getAppService<FormatService>()

    override fun map(value: OfflineMapFile): ListItem {
        return ListItem(
            value.id,
            value.name,
            icon = ResourceListIcon(
                R.drawable.ic_file,
                Resources.androidTextColorSecondary(context)
            ),
            subtitle = formatter.join(
                formatter.formatOfflineMapFileTypeName(value.type),
                formatter.formatFileSize(value.sizeBytes),
                separator = FormatService.Separator.Dot
            ),
            trailingIcon = ResourceListIcon(
                if (value.visible) {
                    R.drawable.ic_visible
                } else {
                    R.drawable.ic_not_visible
                },
                Resources.androidTextColorSecondary(context),
                onClick = {
                    actionHandler(value, OfflineMapFileAction.ToggleVisibility)
                }
            ),
            menu = listOfNotNull(
                ListMenuItem(context.getString(R.string.rename)) {
                    actionHandler(value, OfflineMapFileAction.Rename)
                },
                ListMenuItem(context.getString(R.string.delete)) {
                    actionHandler(value, OfflineMapFileAction.Delete)
                },
            )
        )
    }
}
