package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
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

class PathNavigationStore {
    private val prefs = getAppService<SessionStateSubsystem>().preferences

    fun hasSavedRoute(): Boolean = prefs.getString(SESSION_KEY) != null

    fun save(session: PathNavigationSession) {
        prefs.putString(SESSION_KEY, JsonConvert.toJson(session))
    }

    fun load(): PathNavigationSession? {
        val saved = prefs.getString(SESSION_KEY) ?: return null
        return requireNotNull(JsonConvert.fromJson<PathNavigationSession>(saved))
    }

    fun clear() {
        prefs.remove(SESSION_KEY)
    }

    private companion object {
        const val SESSION_KEY = "path_navigation_route"
    }
}
