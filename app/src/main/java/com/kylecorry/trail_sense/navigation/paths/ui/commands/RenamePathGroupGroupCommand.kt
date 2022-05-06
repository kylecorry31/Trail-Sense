package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.alerts.CoroutineAlerts
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain


class RenamePathGroupGroupCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context)
) : ISuspendPathGroupCommand {

    override suspend fun execute(group: PathGroup) {
        val newName = onMain {
            CoroutineAlerts.text(
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