package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathMetadata
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.CoroutineAlerts
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain


class CreatePathCommand(
    private val context: Context,
    private val pathService: IPathService = PathService.getInstance(context),
    private val pathPreferences: IPathPreferences = UserPreferences(context).navigation
) {

    suspend fun execute(parentId: Long?): Long? {
        val name = onMain {
            CoroutineAlerts.text(
                context,
                context.getString(R.string.path),
                hint = context.getString(R.string.name)
            )
        } ?: return null

        return onIO {
            pathService.addPath(
                Path(
                    0,
                    name,
                    pathPreferences.defaultPathStyle,
                    PathMetadata.empty,
                    parentId = parentId
                )
            )
        }
    }
}