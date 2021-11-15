package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.io.IOService
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathGPXConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant


class ExportPathCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val gpxService: IOService<GPXData>,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        lifecycleScope.launch {
            val waypoints = pathService.getWaypoints(path.id)
            val gpx = PathGPXConverter().toGPX(path.name, waypoints)
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)
            withContext(Dispatchers.Main) {
                if (success) {
                    Alerts.toast(
                        context,
                        context.getString(R.string.path_exported)
                    )
                } else {
                    Alerts.toast(
                        context,
                        context.getString(R.string.export_path_error)
                    )
                }
            }
        }
    }
}