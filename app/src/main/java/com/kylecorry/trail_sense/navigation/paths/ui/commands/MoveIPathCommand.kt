package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.onIO

class MoveIPathCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context)
) {

    suspend fun execute(path: IPath): PathGroup? {
        val newGroup = CustomUiUtils.pickPathGroup(
            context,
            null,
            context.getString(R.string.move),
            initialGroup = path.parentId,
            groupsToExclude = listOfNotNull(if (path is PathGroup) path.id else null)
        )

        if (newGroup == -1L || newGroup == path.parentId) {
            return onIO {
                pathService.getGroup(path.parentId)
            }
        }

        return onIO {
            if (path is PathGroup) {
                pathService.addGroup(path.copy(parentId = newGroup))
            } else {
                pathService.addPath((path as Path).copy(parentId = newGroup))
            }

            pathService.getGroup(newGroup)
        }
    }

}