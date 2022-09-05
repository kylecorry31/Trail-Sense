package com.kylecorry.trail_sense.tools.packs.ui.mappers

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ListItemMapper
import com.kylecorry.ceres.list.ListMenuItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.packs.domain.Pack

enum class PackAction {
    Rename,
    Copy,
    Delete,
    Open
}


class PackListItemMapper(
    private val context: Context,
    private val actionHandler: (Pack, PackAction) -> Unit
) : ListItemMapper<Pack> {
    override fun map(value: Pack): ListItem {
        return ListItem(
            value.id,
            value.name,
            icon = ResourceListIcon(
                R.drawable.ic_tool_pack,
                tint = Resources.androidTextColorSecondary(context)
            ),
            menu = listOf(
                ListMenuItem(context.getString(R.string.rename)) {
                    actionHandler(
                        value,
                        PackAction.Rename
                    )
                },
                ListMenuItem(context.getString(R.string.copy)) {
                    actionHandler(
                        value,
                        PackAction.Copy
                    )
                },
                ListMenuItem(context.getString(R.string.delete)) {
                    actionHandler(
                        value,
                        PackAction.Delete
                    )
                },
            )
        ) {
            actionHandler(value, PackAction.Open)
        }
    }
}