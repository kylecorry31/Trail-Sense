package com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapState
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapType
import java.time.Instant

data class TrailMap(
    override val id: Long,
    override val name: String,
    override val files: List<OfflineMapFile>,
    override val createdOn: Instant,
    override val bounds: CoordinateBounds?,
    val attribution: String?,
    override val visible: Boolean,
    override val parentId: Long? = null,
    val isAvailable: Boolean = true
) : OfflineMap {
    override val type = OfflineMapType.Trail
    override val isGroup = false
    override val count: Int? = null
    override val state = if (bounds == null) OfflineMapState.Draft else OfflineMapState.Ready
    val mapFile = files.single { it.role == FILE_ROLE_MAPSFORGE_MAP }

    companion object {
        const val FILE_ROLE_MAPSFORGE_MAP = "mapsforge-map"
    }
}
