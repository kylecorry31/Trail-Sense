package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DeletePathCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        Alerts.dialog(
            context,
            context.getString(R.string.delete_path),
            context.resources.getQuantityString(
                R.plurals.waypoints_to_be_deleted,
                path.metadata.waypoints,
                path.metadata.waypoints
            )
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launchWhenResumed {
                    withContext(Dispatchers.IO) {
                        pathService.deletePath(path)
                    }
                }
            }
        }
    }
}