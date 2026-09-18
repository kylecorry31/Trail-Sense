package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.PathNavigationMode
import com.kylecorry.trail_sense.tools.navigation.domain.PathRouteBuilder
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class PathNavigationStore(context: Context) {
    private val prefs = SharedPreferences(context, SESSION_STATE)
    private val paths = PathService.getInstance(context)
    private var active: Destination.Path? = null
    private var revision = 0L

    @Synchronized
    fun save(
        path: Path,
        route: List<PathPoint>,
        start: Coordinate,
        mode: PathNavigationMode,
        destinationPointId: Long?
    ): Destination.Path {
        revision++
        val state = PathNavigationState(
            id = path.id,
            mode = mode.name,
            destinationPointId = destinationPointId,
            startLat = start.latitude,
            startLon = start.longitude
        )
        prefs.remove(PROGRESS_KEY)
        prefs.putString(ROUTE_KEY, JsonConvert.toJson(state))
        return attach(Destination.Path(path, route))
    }

    suspend fun restore(): Destination.Path? {
        val (saved, progressSnapshot, restoreRevision) = synchronized(this) {
            Triple(prefs.getString(ROUTE_KEY) ?: return null, prefs.getString(PROGRESS_KEY), revision)
        }
        return try {
            val state = requireNotNull(JsonConvert.fromJson<PathNavigationState>(saved))
            val id = state.id
            val path = requireNotNull(paths.getPath(id))
            val originalPoints = paths.getWaypoints(id)
            require(originalPoints.isNotEmpty())
            currentCoroutineContext().ensureActive()
            val start = Coordinate(state.startLat, state.startLon)
            val route = PathRouteBuilder.prepare(
                originalPoints, start, PathNavigationMode.valueOf(state.mode), state.destinationPointId
            )
            val destination = Destination.Path(path, route)
            progressSnapshot?.let {
                JsonConvert.fromJson<PathNavigationProgress>(it)?.let { progress ->
                    destination.route.restoreProgress(
                        progress.progress,
                        Coordinate(progress.lat, progress.lon)
                    )
                }
            }
            currentCoroutineContext().ensureActive()
            synchronized(this) {
                if (revision == restoreRevision) attach(destination) else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            synchronized(this) {
                if (revision == restoreRevision) clear()
            }
            null
        }
    }

    @Synchronized
    fun clear() {
        revision++
        active = null
        prefs.remove(ROUTE_KEY)
        prefs.remove(PROGRESS_KEY)
    }

    private fun attach(destination: Destination.Path): Destination.Path {
        active = destination
        destination.route.onProgressChanged = { fraction, location ->
            synchronized(this) {
                if (active === destination) {
                    val progress = PathNavigationProgress(fraction, location.latitude, location.longitude)
                    prefs.putString(PROGRESS_KEY, JsonConvert.toJson(progress))
                }
            }
        }
        return destination
    }

    private data class PathNavigationState(
        val id: Long,
        val mode: String,
        val destinationPointId: Long?,
        val startLat: Double,
        val startLon: Double
    ) : ProguardIgnore

    private data class PathNavigationProgress(
        val progress: Float,
        val lat: Double,
        val lon: Double
    ) : ProguardIgnore

    private companion object {
        const val SESSION_STATE = "session_state"
        const val ROUTE_KEY = "path_navigation_route"
        const val PROGRESS_KEY = "path_navigation_progress"
    }
}
