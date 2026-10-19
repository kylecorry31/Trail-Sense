package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.tools.paths.domain.Path

// Coordinates are stored as plain numbers since the library's Coordinate class is not kept by R8
data class PathNavigationSession(
    val pathId: Long,
    val reversed: Boolean,
    val destinationPointId: Long?,
    val startLatitude: Double,
    val startLongitude: Double,
    val progress: PathNavigationSessionProgress? = null
) : ProguardIgnore {
    val start: Coordinate
        get() = Coordinate(startLatitude, startLongitude)
}

data class PathNavigationSessionProgress(
    val distance: Float,
    val latitude: Double,
    val longitude: Double
) : ProguardIgnore {
    val location: Coordinate
        get() = Coordinate(latitude, longitude)
}

class PathNavigationStore {
    private val prefs = getAppService<SessionStateSubsystem>().preferences

    fun hasSavedRoute(): Boolean = prefs.getString(ROUTE_KEY) != null

    fun save(
        path: Path,
        start: Coordinate,
        reversed: Boolean,
        destinationPointId: Long?
    ) {
        val session = PathNavigationSession(
            path.id,
            reversed,
            destinationPointId,
            start.latitude,
            start.longitude
        )
        prefs.remove(PROGRESS_KEY)
        prefs.putString(ROUTE_KEY, JsonConvert.toJson(session))
    }

    fun load(): PathNavigationSession? {
        val saved = prefs.getString(ROUTE_KEY) ?: return null
        val session = requireNotNull(JsonConvert.fromJson<PathNavigationSession>(saved))
        val progress = prefs.getString(PROGRESS_KEY)
            ?.let { JsonConvert.fromJson<PathNavigationSessionProgress>(it) }
        return session.copy(progress = progress)
    }

    fun clear() {
        prefs.remove(ROUTE_KEY)
        prefs.remove(PROGRESS_KEY)
    }

    fun saveProgress(distance: Float, location: Coordinate) {
        val progress = PathNavigationSessionProgress(distance, location.latitude, location.longitude)
        prefs.putString(PROGRESS_KEY, JsonConvert.toJson(progress))
    }

    private companion object {
        const val ROUTE_KEY = "path_navigation_route"
        const val PROGRESS_KEY = "path_navigation_progress"
    }
}
