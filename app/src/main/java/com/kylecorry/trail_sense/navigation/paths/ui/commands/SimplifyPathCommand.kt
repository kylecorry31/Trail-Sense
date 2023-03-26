package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathSimplificationQuality
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SimplifyPathCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        Pickers.item(
            context, context.getString(R.string.simplification_quality), listOf(
                context.getString(R.string.high),
                context.getString(R.string.moderate),
                context.getString(R.string.low)
            )
        ) {
            if (it != null) {
                val quality = listOf(
                    PathSimplificationQuality.High,
                    PathSimplificationQuality.Medium,
                    PathSimplificationQuality.Low
                )[it]
                lifecycleOwner.inBackground {
                    val loading = withContext(Dispatchers.Main) {
                        Alerts.loading(context, context.getString(R.string.simplifying))
                    }
                    val deleted = withContext(Dispatchers.IO) {
                        pathService.simplifyPath(path.id, quality)
                    }
                    withContext(Dispatchers.Main) {
                        loading.dismiss()
                        Alerts.toast(
                            context,
                            context.resources.getQuantityString(
                                R.plurals.waypoints_deleted,
                                deleted,
                                deleted
                            )
                        )
                    }
                }
            }
        }
    }
}