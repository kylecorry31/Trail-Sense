package com.kylecorry.trail_sense.tools.offline_maps.domain.sort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.GroupMapper
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap

class MapCreateTimeMapper(override val loader: IGroupLoader<IMap>) : GroupMapper<IMap, Long, Long>() {

    override suspend fun getValue(item: IMap): Long {
        val time = when (item) {
            is PhotoMap -> item.createdOn
            is VectorMap -> item.createdOn
            else -> null
        }
        return time?.toEpochMilli() ?: item.id
    }

    override suspend fun aggregate(values: List<Long>): Long {
        return values.maxOrNull() ?: 0
    }

}
