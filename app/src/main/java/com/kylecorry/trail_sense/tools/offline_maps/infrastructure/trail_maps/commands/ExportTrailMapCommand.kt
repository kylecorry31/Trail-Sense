package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap

class ExportTrailMapCommand(
    private val exporter: ExportService<TrailMap>,
    private val loading: ILoadingIndicator
) {

    suspend fun execute(map: TrailMap): Boolean = onIO {
        onMain {
            loading.show()
        }
        val success = exporter.export(map, "${map.name.slugify()}.map")
        onMain {
            loading.hide()
        }
        success
    }
}
