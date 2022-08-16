package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.LoessFilter2D

object DataUtils {

    fun smooth(data: List<Vector2>, smoothness: Float = 0.06f): List<Vector2> {
        val filter = LoessFilter2D(smoothness, 1, minimumSpanSize = if (smoothness == 0f) 0 else 10)
        return filter.filter(data)
    }

    fun <T> smooth(
        data: List<T>,
        smoothness: Float = 0.1f,
        select: (index: Int, value: T) -> Vector2,
        merge: (value: T, smoothed: Vector2) -> T
    ): List<T> {
        val smoothed = smooth(data.mapIndexed { index, value -> select(index, value) }, smoothness)
        return data.zip(smoothed).map {
            merge(it.first, it.second)
        }
    }

}