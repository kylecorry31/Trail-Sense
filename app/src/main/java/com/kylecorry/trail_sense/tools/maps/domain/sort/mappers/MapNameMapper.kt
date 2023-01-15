package com.kylecorry.trail_sense.tools.maps.domain.sort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper
import com.kylecorry.trail_sense.tools.maps.domain.IMap

class MapNameMapper : ISuspendMapper<IMap, String> {
    override suspend fun map(item: IMap): String {
        return item.name
    }
}