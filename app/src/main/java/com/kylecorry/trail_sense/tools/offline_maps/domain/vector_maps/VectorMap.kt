package com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import java.time.Instant

data class VectorMap(
    override val id: Long,
    override val name: String,
    val type: VectorMapFileType,
    val path: String,
    val sizeBytes: Long,
    val createdOn: Instant,
    val bounds: CoordinateBounds?,
    val attribution: String?,
    val visible: Boolean,
    override val parentId: Long? = null,
    val isAvailable: Boolean = true
) : IMap {
    override val isGroup = false
    override val count: Int? = null
    val isExternal: Boolean = path.startsWith(FileSubsystem.SCHEME_CONTENT)
}
