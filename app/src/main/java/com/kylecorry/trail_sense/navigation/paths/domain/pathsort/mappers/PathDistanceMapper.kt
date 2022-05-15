package com.kylecorry.trail_sense.navigation.paths.domain.pathsort.mappers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.shared.grouping.mapping.GroupMapper
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader

class PathDistanceMapper(
    override val loader: IGroupLoader<IPath>,
    private val locationProvider: () -> Coordinate
) : GroupMapper<IPath, Float, Float>() {

    override suspend fun getValue(item: IPath): Float {
        val center = (item as Path).metadata.bounds.center
        return center.distanceTo(locationProvider.invoke())
    }

    override suspend fun aggregate(values: List<Float>): Float {
        return values.minOrNull() ?: Float.POSITIVE_INFINITY
    }

}