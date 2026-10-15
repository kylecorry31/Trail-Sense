package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.tools.paths.domain.Path

data class PathNavigationSession(
    val pathId: Long,
    val reversed: Boolean,
    val destinationPointId: Long?,
    val start: Coordinate,
    val progress: PathNavigationSessionProgress?
)

data class PathNavigationSessionProgress(val distance: Float, val location: Coordinate)

class PathNavigationStore {
    private val prefs = getAppService<SessionStateSubsystem>().preferences

    fun hasSavedRoute(): Boolean = prefs.getString(ROUTE_KEY) != null

    fun getSavedPathId(): Long? {
        val saved = prefs.getString(ROUTE_KEY) ?: return null
        return JsonConvert.fromJson<PathNavigationState>(saved)?.id
    }

    suspend fun save(
        path: Path,
        start: Coordinate,
        reversed: Boolean,
        destinationPointId: Long?
    ) = onIO {
        val state = PathNavigationState(
            id = path.id,
            reversed = reversed,
            destinationPointId = destinationPointId,
            startLat = start.latitude,
            startLon = start.longitude
        )
        synchronized(this@PathNavigationStore) {
            prefs.remove(PROGRESS_KEY)
            prefs.putString(ROUTE_KEY, JsonConvert.toJson(state))
        }
    }

    suspend fun load(): PathNavigationSession? = onIO {
        synchronized(this@PathNavigationStore) {
            val saved = prefs.getString(ROUTE_KEY) ?: return@synchronized null
            val state = requireNotNull(JsonConvert.fromJson<PathNavigationState>(saved))
            val progress = prefs.getString(PROGRESS_KEY)
                ?.let { JsonConvert.fromJson<PathNavigationProgress>(it) }
                ?.let { PathNavigationSessionProgress(it.distance, Coordinate(it.lat, it.lon)) }
            PathNavigationSession(
                state.id,
                state.reversed,
                state.destinationPointId,
                Coordinate(state.startLat, state.startLon),
                progress
            )
        }
    }

    @Synchronized
    fun clear() {
        prefs.remove(ROUTE_KEY)
        prefs.remove(PROGRESS_KEY)
    }

    suspend fun saveProgress(
        distance: Float,
        location: Coordinate,
        isCurrent: () -> Boolean
    ) = onIO {
        val progress = PathNavigationProgress(distance, location.latitude, location.longitude)
        synchronized(this@PathNavigationStore) {
            if (!isCurrent()) return@synchronized
            prefs.putString(PROGRESS_KEY, JsonConvert.toJson(progress))
        }
    }

    private data class PathNavigationState(
        val id: Long,
        val reversed: Boolean,
        val destinationPointId: Long?,
        val startLat: Double,
        val startLon: Double
    ) : ProguardIgnore

    private data class PathNavigationProgress(
        val distance: Float,
        val lat: Double,
        val lon: Double
    ) : ProguardIgnore

    private companion object {
        const val ROUTE_KEY = "path_navigation_route"
        const val PROGRESS_KEY = "path_navigation_progress"
    }
}
