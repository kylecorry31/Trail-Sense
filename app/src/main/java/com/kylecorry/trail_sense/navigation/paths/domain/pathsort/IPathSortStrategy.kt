package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.Path

interface IPathSortStrategy {
    fun sort(paths: List<Path>): List<Path>
}