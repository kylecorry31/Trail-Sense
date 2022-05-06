package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.alerts.CoroutineAlerts
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain


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