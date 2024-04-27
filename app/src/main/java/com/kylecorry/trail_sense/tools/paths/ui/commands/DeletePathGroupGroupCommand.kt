package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService


class DeletePathGroupGroupCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context)
) : ISuspendPathGroupCommand {

    override suspend fun execute(group: PathGroup) {
        val cancelled = onMain {
            CoroutineAlerts.dialog(
                context,
                context.getString(R.string.delete),
                context.getString(R.string.delete_path_group_message, group.name)
            )
        }

        if (cancelled) {
            return
        }

        onIO {
            pathService.deleteGroup(group)
        }
    }
}