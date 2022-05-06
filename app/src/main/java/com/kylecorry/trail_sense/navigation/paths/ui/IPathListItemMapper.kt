package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.shared.lists.ListItem
import com.kylecorry.trail_sense.shared.lists.ListItemMapper

class IPathListItemMapper(
    context: Context,
    pathHandler: (Path, PathAction) -> Unit,
    groupHandler: (PathGroup, PathGroupAction) -> Unit
) : ListItemMapper<IPath> {

    private val pathMapper = PathListItemMapper(context, pathHandler)
    private val groupMapper = PathGroupListItemMapper(context, groupHandler)

    override fun map(value: IPath): ListItem {
        return if (value is Path) {
            pathMapper.map(value)
        } else {
            groupMapper.map(value as PathGroup)
        }
    }
}