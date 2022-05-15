package com.kylecorry.trail_sense.navigation.paths.domain.pathsort.mappers

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.shared.grouping.mapping.GroupMapper
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader

class PathIdMapper(override val loader: IGroupLoader<IPath>) : GroupMapper<IPath, Long, Long>() {

    override suspend fun getValue(item: IPath): Long {
        return item.id
    }

    override suspend fun aggregate(values: List<Long>): Long {
        return values.maxOrNull() ?: 0
    }

}