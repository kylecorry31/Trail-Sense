package com.kylecorry.trail_sense.tools.maps.infrastructure.commands

import com.github.slugify.Slugify
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.tools.maps.domain.Map

class ExportMapCommand(private val exporter: ExportService<Map>, private val loading: ILoadingIndicator) {

    suspend fun execute(map: Map) = onIO {
        val slugify = Slugify()
        onMain {
            loading.show()
        }
        exporter.export(map, "${slugify.slugify(map.name)}.pdf")
        onMain {
            loading.hide()
        }
    }

}