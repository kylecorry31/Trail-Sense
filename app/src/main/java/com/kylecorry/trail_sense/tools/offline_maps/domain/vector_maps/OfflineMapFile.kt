package com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import java.time.Instant

data class OfflineMapFile(
    override val id: Long,
    override val name: String,
    val type: OfflineMapFileType,
    val path: String,
    val sizeBytes: Long,
    val createdOn: Instant,
    val bounds: CoordinateBounds?,
    val attribution: String?,
    val visible: Boolean,
    override val parentId: Long? = null
) : IMap {
    override val isGroup = false
    override val count: Int? = null
}
