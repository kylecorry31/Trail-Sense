package com.kylecorry.trail_sense.tools.map.domain

data class OfflineMapFileGroup(
    override val id: Long,
    override val name: String,
    override val parentId: Long? = null,
    override val count: Int? = 0
) : IOfflineMapFile {
    override val isGroup = true
}
