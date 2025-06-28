package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.commands

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.text.slugify
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

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