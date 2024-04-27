package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathPickers
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService

class MoveIPathCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context)
) {

    suspend fun execute(path: IPath): PathGroup? {

        val result = PathPickers.pickGroup(
            context,
            null,
            context.getString(R.string.move),
            initialGroup = path.parentId,
            filter = { it.filter { path !is PathGroup || path.id != it.id } }
        )

        val newGroup = result.second?.id

        if (result.first || newGroup == path.parentId) {
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

            result.second
        }
    }

}