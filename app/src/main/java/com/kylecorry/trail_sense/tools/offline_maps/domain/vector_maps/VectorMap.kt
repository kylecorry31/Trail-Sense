package com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import java.time.Instant

data class VectorMap(
    override val id: Long,
    override val name: String,
    val type: VectorMapFileType,
    val files: List<OfflineMapFile>,
    val createdOn: Instant,
    val bounds: CoordinateBounds?,
    val attribution: String?,
    val visible: Boolean,
    override val parentId: Long? = null,
    val isAvailable: Boolean = true
) : IMap {
    override val isGroup = false
    override val count: Int? = null
    val mapFile = files.single { it.role == FILE_ROLE_MAPSFORGE_MAP }
    val isExternal = files.any { it.isExternal }
    val fileSizeBytes = files.sumOf { it.sizeBytes }

    companion object {
        const val FILE_ROLE_MAPSFORGE_MAP = "mapsforge-map"
    }
}
