package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.extensions.withCancelableProgress
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReplacePathElevationsCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        Alerts.dialog(
            context,
            context.getString(R.string.replace_elevations),
            context.getString(R.string.replace_elevations_confirmation)
        ) { cancelled ->
            if (cancelled) {
                return@dialog
            }

            lifecycleOwner.inBackground {
                var elevations = emptyList<PathPoint>()
                var job: Job? = null
                Alerts.withCancelableProgress(
                    context,
                    context.getString(R.string.loading),
                    onCancel = { job?.cancel() }
                ) { setProgress ->
                    job = launch {
                        val points = pathService.getWaypoints(path.id)
                        elevations = points.mapIndexed { index, point ->
                            val elevation = DEM.getElevation(point.coordinate).elevation
                            setProgress((index + 1) / points.size.coerceAtLeast(1).toFloat())
                            point.copy(elevation = elevation)
                        }
                    }
                    job.join()
                }

                if (job?.isCancelled == true) {
                    return@inBackground
                }

                val loading = onMain {
                    Alerts.loading(context, context.getString(R.string.saving))
                }
                onIO {
                    pathService.replaceWaypoints(elevations)
                }
                onMain {
                    loading.dismiss()
                }
            }
        }
    }
}
