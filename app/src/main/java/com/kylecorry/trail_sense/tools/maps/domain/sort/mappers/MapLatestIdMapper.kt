package com.kylecorry.trail_sense.tools.maps.domain.sort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.GroupMapper
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.tools.maps.domain.IMap

class MapLatestIdMapper(override val loader: IGroupLoader<IMap>) : GroupMapper<IMap, Long, Long>() {

    override suspend fun getValue(item: IMap): Long {
        return item.id
    }

    override suspend fun aggregate(values: List<Long>): Long {
        return values.maxOrNull() ?: 0
    }

}