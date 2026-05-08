package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap

class MapNameMapper : ISuspendMapper<IMap, String> {
    override suspend fun map(item: IMap): String {
        return item.name
    }
}
