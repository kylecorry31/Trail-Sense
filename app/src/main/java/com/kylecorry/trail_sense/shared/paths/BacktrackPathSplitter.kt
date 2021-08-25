package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Path
import com.kylecorry.trailsensecore.domain.geo.PathOwner
import com.kylecorry.trailsensecore.domain.geo.PathPoint

// TODO: This should just group the points by path, not create a path
class BacktrackPathSplitter(private val prefs: UserPreferences) : IBacktrackPathSplitter {
    override fun split(points: List<PathPoint>): List<Path> {
        val grouped = points.groupBy { it.pathId }
        val color = prefs.navigation.backtrackPathColor.color
        val style = prefs.navigation.backtrackPathStyle
        return grouped.map {
            Path(
                it.key,
                "",
                it.value,
                color,
                style,
                true,
                PathOwner.Backtrack
            )
        }
    }
}