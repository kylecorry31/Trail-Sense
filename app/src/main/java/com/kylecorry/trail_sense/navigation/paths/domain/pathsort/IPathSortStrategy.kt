package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.shared.paths.Path

interface IPathSortStrategy {
    fun sort(paths: List<Path>): List<Path>
}