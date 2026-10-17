package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Coordinate

enum class PathDirection {
    ToStart,
    ToEnd
}

object PathDirectionRecommender {

    /**
     * Picks the direction the user most likely wants to follow, or null if neither is more likely.
     * @param isRecording true if the path is the one Backtrack is currently recording, so its end is where the user is
     */
    fun recommend(
        start: Coordinate,
        end: Coordinate,
        location: Coordinate,
        isRecording: Boolean,
        isLoop: Boolean
    ): PathDirection? {
        // A loop's start and end are the same place, so neither direction is closer
        if (isLoop) {
            return null
        }

        if (isRecording) {
            return PathDirection.ToStart
        }

        val distanceToStart = location.distanceTo(start)
        val distanceToEnd = location.distanceTo(end)
        return when {
            distanceToEnd < distanceToStart -> PathDirection.ToStart
            distanceToStart < distanceToEnd -> PathDirection.ToEnd
            else -> null
        }
    }
}
