package com.kylecorry.trail_sense.tools.navigation.infrastructure

import androidx.annotation.Keep
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSession
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSessionProgress
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection

class PathNavigationStore {
    private val prefs = getAppService<SessionStateSubsystem>().preferences

    fun hasSavedRoute(): Boolean = prefs.getString(SESSION_KEY) != null

    fun save(session: PathNavigationSession) {
        val followDirection = session.follow as? PathFollow.Direction
        val followToPoint = session.follow as? PathFollow.ToPoint
        val storedSession = StoredPathNavigationSession(
            session.pathId,
            followDirection?.direction == PathDirection.ToStart,
            followToPoint?.pointId,
            session.start.latitude,
            session.start.longitude,
            session.progress?.let {
                StoredPathNavigationSessionProgress(
                    it.distanceAlong.meters().value,
                    it.location.latitude,
                    it.location.longitude
                )
            }
        )
        prefs.putString(SESSION_KEY, JsonConvert.toJson(storedSession))
    }

    fun load(): PathNavigationSession? {
        val saved = prefs.getString(SESSION_KEY) ?: return null
        val storedSession = requireNotNull(JsonConvert.fromJson<StoredPathNavigationSession>(saved))
        return PathNavigationSession(
            storedSession.pathId,
            storedSession.destinationPointId?.let { PathFollow.ToPoint(it) }
                ?: PathFollow.Direction(
                    if (storedSession.reversed) PathDirection.ToStart else PathDirection.ToEnd
                ),
            Coordinate(storedSession.startLatitude, storedSession.startLongitude),
            storedSession.progress?.let {
                PathNavigationSessionProgress(
                    Distance.meters(it.distance),
                    Coordinate(it.latitude, it.longitude)
                )
            }
        )
    }

    fun clear() {
        prefs.remove(SESSION_KEY)
    }

    private companion object {
        const val SESSION_KEY = "path_navigation_route"
    }
}

@Keep
private data class StoredPathNavigationSession(
    val pathId: Long,
    val reversed: Boolean,
    val destinationPointId: Long?,
    val startLatitude: Double,
    val startLongitude: Double,
    val progress: StoredPathNavigationSessionProgress? = null
)

@Keep
private data class StoredPathNavigationSessionProgress(
    val distance: Float,
    val latitude: Double,
    val longitude: Double
)
