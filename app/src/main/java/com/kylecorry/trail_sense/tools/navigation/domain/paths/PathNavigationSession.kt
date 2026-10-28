package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection

data class PathNavigationSession(
    val pathId: Long,
    val reversed: Boolean,
    val destinationPointId: Long?,
    val startLatitude: Double,
    val startLongitude: Double,
    val progress: PathNavigationSessionProgress? = null
) : ProguardIgnore {
    val follow: PathFollow
        get() = destinationPointId?.let { PathFollow.ToPoint(it) }
            ?: PathFollow.Direction(if (reversed) PathDirection.ToStart else PathDirection.ToEnd)

    val start: Coordinate
        get() = Coordinate(startLatitude, startLongitude)

    companion object {
        fun of(pathId: Long, follow: PathFollow, start: Coordinate): PathNavigationSession {
            return PathNavigationSession(
                pathId,
                follow is PathFollow.Direction && follow.direction == PathDirection.ToStart,
                (follow as? PathFollow.ToPoint)?.pointId,
                start.latitude,
                start.longitude
            )
        }
    }
}

data class PathNavigationSessionProgress(
    val distance: Float,
    val latitude: Double,
    val longitude: Double
) : ProguardIgnore {
    val location: Coordinate
        get() = Coordinate(latitude, longitude)

    companion object {
        fun of(distance: Float, location: Coordinate): PathNavigationSessionProgress {
            return PathNavigationSessionProgress(distance, location.latitude, location.longitude)
        }
    }
}
