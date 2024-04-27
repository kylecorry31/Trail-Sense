package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService


class CreatePathGroupCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context)
) {

    suspend fun execute(parentId: Long?) {
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.group),
                hint = context.getString(R.string.name)
            )
        } ?: return

        onIO {
            pathService.addGroup(PathGroup(0, name, parentId))
        }
    }
}