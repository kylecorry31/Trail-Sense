package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DeletePointCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        Alerts.dialog(
            context,
            context.getString(R.string.delete_waypoint_prompt)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleOwner.inBackground {
                    withContext(Dispatchers.IO) {
                        pathService.deleteWaypoint(point)
                    }
                }
            }
        }
    }
}