package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.provider.PathValueProvider

abstract class AggregationPathSortStrategy<T : Comparable<T>>(
    private val ascending: Boolean = true
) : IPathSortStrategy {

    abstract fun getProvider(): PathValueProvider<T>

    override suspend fun sort(paths: List<IPath>): List<IPath> {
        val provider = getProvider()
        val values = paths.map { provider.get(it) }

        return if (ascending) {
            paths.zip(values).sortedBy { it.second }
        } else {
            paths.zip(values).sortedByDescending { it.second }
        }.map { it.first }
    }
}