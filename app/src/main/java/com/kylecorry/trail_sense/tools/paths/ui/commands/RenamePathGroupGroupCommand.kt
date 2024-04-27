package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService


class RenamePathGroupGroupCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context)
) : ISuspendPathGroupCommand {

    override suspend fun execute(group: PathGroup) {
        val newName = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.rename),
                default = group.name,
                hint = context.getString(R.string.name)
            )
        } ?: return

        onIO {
            pathService.addGroup(group.copy(name = newName))
        }
    }
}