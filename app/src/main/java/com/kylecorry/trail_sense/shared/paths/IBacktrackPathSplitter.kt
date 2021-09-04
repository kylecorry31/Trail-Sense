package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPoint

interface IBacktrackPathSplitter {
    fun split(points: List<PathPoint>): List<Path>
}