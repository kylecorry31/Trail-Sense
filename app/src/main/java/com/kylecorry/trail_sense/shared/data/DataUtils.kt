package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.LoessFilter

object DataUtils {

    fun smooth(data: List<Vector2>, smoothness: Float = 0.06f): List<Vector2> {
        val f = if (smoothness > 0f) {
            if (data.size * smoothness >= 10) {
                smoothness
            } else {
                (10.1f / data.size).coerceAtMost(1f)
            }
        } else {
            0f
        }
        val filter = LoessFilter(f, 1)
        return filter.filter(data)
    }

    fun <T> smooth(
        data: List<T>,
        smoothness: Float = 0.1f,
        select: (T) -> Vector2,
        merge: (T, Vector2) -> T
    ): List<T> {
        val smoothed = smooth(data.map(select), smoothness)
        return data.zip(smoothed).map {
            merge(it.first, it.second)
        }
    }

}