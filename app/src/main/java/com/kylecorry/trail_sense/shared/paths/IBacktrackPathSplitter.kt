package com.kylecorry.trail_sense.shared.paths

interface IBacktrackPathSplitter {
    fun split(points: List<PathPoint>): List<Path>
}