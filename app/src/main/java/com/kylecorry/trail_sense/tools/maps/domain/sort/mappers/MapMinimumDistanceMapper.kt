package com.kylecorry.trail_sense.tools.maps.domain.sort.mappers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.grouping.mapping.GroupMapper
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.tools.maps.domain.IMap
import com.kylecorry.trail_sense.tools.maps.domain.Map

class MapMinimumDistanceMapper(
    override val loader: IGroupLoader<IMap>,
    private val offMapPenalty: Float = 0f,
    private val locationProvider: () -> Coordinate
) : GroupMapper<IMap, Float, Float>() {

    override suspend fun getValue(item: IMap): Float {
        val bounds = if (item is Map) {
            item.boundary()
        } else {
            null
        } ?: return Float.MAX_VALUE
        val location = locationProvider.invoke()
        val onMap = bounds.contains(location)
        return (if (onMap) 0f else offMapPenalty) + location.distanceTo(bounds.center)
    }

    override suspend fun aggregate(values: List<Float>): Float {
        return values.minOrNull() ?: Float.POSITIVE_INFINITY
    }
}