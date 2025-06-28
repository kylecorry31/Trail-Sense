package com.kylecorry.trail_sense.tools.photo_maps.domain

data class MapGroup(
    override val id: Long,
    override val name: String,
    override val parentId: Long? = null,
    override val count: Int? = 0
) : IMap {
    override val isGroup = true
}