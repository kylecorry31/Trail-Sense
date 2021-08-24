package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.trailsensecore.domain.geo.Path
import com.kylecorry.trailsensecore.domain.geo.PathPoint

interface IBacktrackPathSplitter {
    fun split(points: List<PathPoint>): List<Path>
}