package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.domain.paths.ROUTING_SAME_PLACE_TOLERANCE_METERS

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
        isRecording: Boolean
    ): PathDirection? {
        if (isRecording) {
            return PathDirection.ToStart
        }

        // Detect if a path is a loop
        if (start.distanceTo(end) <= ROUTING_SAME_PLACE_TOLERANCE_METERS) {
            return PathDirection.ToEnd
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
