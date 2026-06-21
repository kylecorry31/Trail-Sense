package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import android.net.Uri
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.MapFileTypeUtils
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge.MapsforgeAdapter
import java.time.Instant
import java.util.UUID

class CreateVectorMapFromFileCommand(
    private val name: String
) {
    private val files = getAppService<FileSubsystem>()
    private val service = getAppService<MapService>()
    private val prefs = getAppService<UserPreferences>()

    suspend fun execute(uri: Uri): IMap? = onIO {
        val type = MapFileTypeUtils.getType(uri) ?: return@onIO null
        val path = if (prefs.photoMaps.copyTrailMapsToAppStorage) {
            copyToAppStorage(uri, type) ?: return@onIO null
        } else {
            // Some URIs can't be persisted (ex. shared from another app), so fall back to copying
            val canPersist = tryOrDefault(false) {
                files.acceptPersistentAccess(uri)
                true
            }
            if (canPersist) {
                uri.toString()
            } else {
                throw IllegalStateException("Can't persist access to $uri")
            }
        }

        val info = MapsforgeAdapter.getMapInfo(path) ?: return@onIO null

        val mapFile = VectorMap(
            0,
            name,
            type,
            path,
            files.size(path),
            Instant.now(),
            info.bounds,
            info.attribution,
            visible = true
        )
        val id = service.add(mapFile)
        mapFile.copy(id = id)
    }

    private suspend fun copyToAppStorage(uri: Uri, type: VectorMapFileType): String? {
        val extension = MapFileTypeUtils.getExtension(type)
        val saved =
            files.copyToLocal(uri, OFFLINE_MAPS_DIRECTORY, "${UUID.randomUUID()}.$extension")
                ?: return null
        return files.getLocalPath(saved)
    }

    companion object {
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
