package com.kylecorry.trail_sense.tools.offline_maps.domain.groups

import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

data class MapGroup(
    override val id: Long,
    override val name: String,
    override val parentId: Long? = null,
    override val count: Int? = 0
) : OfflineMapCatalogItem {
    override val isGroup = true
}
