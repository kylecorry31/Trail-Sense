package com.kylecorry.trail_sense.navigation.paths.domain.pathsort.mappers

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.shared.grouping.mapping.GroupMapper
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader

class PathLengthMapper(override val loader: IGroupLoader<IPath>, val maximum: Boolean = true) :
    GroupMapper<IPath, Float, Float>() {

    override suspend fun getValue(item: IPath): Float {
        return (item as Path).metadata.distance.meters().distance
    }

    override suspend fun aggregate(values: List<Float>): Float {
        return if (maximum) {
            values.maxOrNull() ?: Float.NEGATIVE_INFINITY
        } else {
            values.minOrNull() ?: Float.POSITIVE_INFINITY
        }
    }

}