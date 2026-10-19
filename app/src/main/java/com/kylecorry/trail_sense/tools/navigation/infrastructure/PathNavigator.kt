package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathGuidance
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRouteBuilder
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PathNavigator(context: Context) {
    private val prefs = getAppService<UserPreferences>().paths
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()
    private val store = PathNavigationStore()
    private val paths = PathService.getInstance(context)
    private val hikingService = HikingService()
    private val destinationState = MutableStateFlow<Destination.Path?>(null)
    private val restoringState = MutableStateFlow(store.hasSavedRoute())
    val isRestoring = restoringState.asStateFlow()
    // Guards the navigation state and the store, which are changed by restoring, navigating, and cancelling
    private val lock = Any()
    private var lastSavedProgress: Pair<Destination.Path, Coordinate>? = null

    private val restoreJob = CoroutineScope(Dispatchers.Default).launch {
        try {
            val restored = restoreOrNull()
            synchronized(lock) {
                if (!isActive) return@synchronized
                if (restored == null) {
                    store.clear()
                } else {
                    destinationState.value = restored
                }
            }
        } finally {
            restoringState.value = false
        }
    }

    val destination: Flow<Destination.Path?> = destinationState

    private suspend fun restoreOrNull(): Destination.Path? {
        return try {
            restore()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun restore(): Destination.Path? {
        val session = store.load() ?: return null
        val path = paths.getPath(session.pathId) ?: return null
        val points = hikingService.correctElevations(
            paths.getWaypoints(session.pathId).sortedBy { it.id },
            prefs.elevationSmoothing
        )
        currentCoroutineContext().ensureActive()
        val route = buildRoute(points, session.start, session.reversed, session.destinationPointId) ?: return null
        val destination = Destination.Path(path, route)
        session.progress?.let { destination.route.restorePosition(it.distance, it.location) }
        destination.route.navigate(locationSubsystem.location, locationSubsystem.locationAccuracy)
        return destination
    }

    init {
        Tools.subscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED) {
            destinationState.value?.let { destination ->
                tryOrLog {
                    val location = locationSubsystem.location
                    val guidance = destination.route.navigate(location, locationSubsystem.locationAccuracy)
                    saveProgress(destination, guidance, location)
                }
            }
        }
    }

    /**
     * Starts following the path.
     * @param points the path's points in recorded order, with their elevations already smoothed
     * @return false if the path can't be followed, in which case the current navigation is unchanged
     */
    fun navigate(
        path: Path,
        points: List<PathPoint>,
        reversed: Boolean = false,
        destinationPointId: Long? = null
    ): Boolean {
        val start = locationSubsystem.location
        val route = buildRoute(points, start, reversed, destinationPointId) ?: return false
        val destination = Destination.Path(path, route)
        destination.route.navigate(start, locationSubsystem.locationAccuracy)
        synchronized(lock) {
            cancel()
            store.save(path, start, reversed, destinationPointId)
            destinationState.value = destination
        }
        return true
    }

    private fun buildRoute(
        points: List<PathPoint>,
        start: Coordinate,
        reversed: Boolean,
        destinationPointId: Long?
    ): List<PathPoint>? {
        if (points.isEmpty()) return null
        if (destinationPointId != null && points.none { it.id == destinationPointId }) return null
        return PathRouteBuilder.buildRoute(
            points,
            start,
            reversed,
            destinationPointId
        )
    }

    private fun saveProgress(destination: Destination.Path, guidance: PathGuidance, location: Coordinate) {
        val lastLocation = lastSavedProgress?.takeIf { it.first === destination }?.second
        if (lastLocation != null && lastLocation.distanceTo(location) < SAVE_PROGRESS_MIN_DISTANCE_METERS) {
            return
        }
        lastSavedProgress = destination to location
        synchronized(lock) {
            if (destinationState.value === destination) {
                store.saveProgress(guidance.distanceAlong, location)
            }
        }
    }

    fun cancel() {
        synchronized(lock) {
            restoreJob.cancel()
            restoringState.value = false
            destinationState.value = null
            store.clear()
        }
    }

    fun cancel(pathId: Long) {
        if (destinationState.value?.path?.id == pathId) {
            cancel()
        }
    }

    suspend fun awaitRestore() {
        restoreJob.join()
    }

    private companion object {
        const val SAVE_PROGRESS_MIN_DISTANCE_METERS = 25f
    }
}
