package com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps

data class OfflineMapFileGroup(
    override val id: Long,
    override val name: String,
    override val parentId: Long? = null,
    override val count: Int? = 0
) : IOfflineMapFile {
    override val isGroup = true
}
