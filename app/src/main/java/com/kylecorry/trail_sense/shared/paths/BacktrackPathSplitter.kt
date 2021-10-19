package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.trail_sense.shared.UserPreferences

class BacktrackPathSplitter(private val prefs: UserPreferences) : IBacktrackPathSplitter {
    override fun split(points: List<PathPoint>): List<Path> {
        val grouped = points.groupBy { it.pathId }
        val style = prefs.navigation.defaultPathStyle
        return grouped.map {
            Path(
                it.key,
                "",
                it.value,
                style.color,
                style.line,
                true,
                PathOwner.Backtrack
            )
        }
    }
}