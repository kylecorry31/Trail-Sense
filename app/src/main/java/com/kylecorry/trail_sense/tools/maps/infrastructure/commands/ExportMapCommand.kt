package com.kylecorry.trail_sense.tools.maps.infrastructure.commands

import com.kylecorry.trail_sense.shared.Slugify.slugify
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class ExportMapCommand(
    private val exporter: ExportService<PhotoMap>,
    private val loading: ILoadingIndicator
) {

    suspend fun execute(map: PhotoMap): Boolean = onIO {
        onMain {
            loading.show()
        }
        val success = exporter.export(map, "${map.name.slugify()}.pdf")
        onMain {
            loading.hide()
        }
        success
    }

}