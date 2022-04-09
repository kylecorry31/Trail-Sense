package com.kylecorry.trail_sense.tools.maps.infrastructure.commands

import com.github.slugify.Slugify
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.tools.maps.domain.Map

class ExportMapCommand(private val exporter: ExportService<Map>) {

    suspend fun execute(map: Map) = onIO {
        val slugify = Slugify()
        exporter.export(map, "${slugify.slugify(map.name)}.pdf")
    }

}