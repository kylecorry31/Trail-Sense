package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import android.net.Uri
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.MapFileTypeUtils
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.attribution.OfflineMapAttributionExtractorFactory
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.bounds.OfflineMapBoundsCalculatorFactory
import java.time.Instant
import java.util.UUID

class CreateVectorMapFromFileCommand(
    private val repo: MapRepo,
    private val name: String
) {
    private val files = getAppService<FileSubsystem>()

    suspend fun execute(uri: Uri): IMap? = onIO {
        val type = MapFileTypeUtils.getType(uri) ?: return@onIO null
        val extension = MapFileTypeUtils.getExtension(type)
        val saved = files.copyToLocal(uri, OFFLINE_MAPS_DIRECTORY, "${UUID.randomUUID()}.$extension")
            ?: return@onIO null
        val mapFile = OfflineMapFile(
            0,
            name,
            type,
            files.getLocalPath(saved),
            saved.length(),
            Instant.now(),
            OfflineMapBoundsCalculatorFactory().getBoundsCalculator(type).getBounds(saved),
            OfflineMapAttributionExtractorFactory().getAttributionExtractor(type)
                .getAttribution(saved),
            visible = true
        )
        val id = repo.add(mapFile)
        mapFile.copy(id = id)
    }

    companion object {
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
