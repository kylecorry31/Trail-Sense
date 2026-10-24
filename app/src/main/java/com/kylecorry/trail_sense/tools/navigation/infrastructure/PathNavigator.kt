package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathGuidance
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRouteBuilder
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

class PathNavigator(context: Context) {
    private val prefs = getAppService<UserPreferences>().paths
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()
    private val store = PathNavigationStore()
    private val paths = PathService.getInstance(context)
    private val hikingService = HikingService()
    private val destinationState = MutableStateFlow<Destination.Path?>(null)
    private val restoringState = MutableStateFlow(false)
    val isRestoring = restoringState.asStateFlow()

    // Guards the navigation state and the store, which are changed by building, navigating, and cancelling
    private val lock = Any()
    private var session: PathNavigationSession? = null
    private var buildJob: Job? = null
    private var lastSavedProgress: Pair<Destination.Path, Coordinate>? = null

    val destination: Flow<Destination.Path?> = destinationState

    init {
        if (store.hasSavedRoute()) {
            synchronized(lock) {
                buildJob = startBuild()
            }
        }
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
     * Starts following the path from the user's current location. The route is built in the
     * background, during which [isRestoring] is true. If the path can't be followed, the navigation
     * is cleared.
     */
    fun navigate(path: Path, follow: PathFollow) {
        val newSession = PathNavigationSession.of(path.id, follow, locationSubsystem.location)
        synchronized(lock) {
            cancel()
            store.save(newSession)
            buildJob = startBuild()
        }
    }

    // The lock must be held
    private fun startBuild(): Job {
        restoringState.value = true
        return CoroutineScope(Dispatchers.Default).launch {
            try {
                val built = buildOrNull()
                synchronized(lock) {
                    if (!isActive) return@synchronized
                    if (built == null) {
                        store.clear()
                    } else {
                        session = built.first
                        destinationState.value = built.second
                    }
                }
            } finally {
                synchronized(lock) {
                    // A newer build keeps the route loading
                    if (buildJob === coroutineContext.job) {
                        restoringState.value = false
                    }
                }
            }
        }
    }

    private suspend fun buildOrNull(): Pair<PathNavigationSession, Destination.Path>? {
        return try {
            build()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun build(): Pair<PathNavigationSession, Destination.Path>? {
        val session = store.load() ?: return null
        val path = paths.getPath(session.pathId) ?: return null
        val points = hikingService.correctElevations(
            paths.getWaypoints(session.pathId).sortedBy { it.id },
            prefs.elevationSmoothing
        )
        currentCoroutineContext().ensureActive()
        val route = PathRouteBuilder.buildRoute(points, session.start, session.follow) ?: return null
        val destination = Destination.Path(path, route)
        session.progress?.let { destination.route.restorePosition(it.distance, it.location) }
        destination.route.navigate(locationSubsystem.location, locationSubsystem.locationAccuracy)
        return session to destination
    }

    private fun saveProgress(destination: Destination.Path, guidance: PathGuidance, location: Coordinate) {
        val lastLocation = lastSavedProgress?.takeIf { it.first === destination }?.second
        if (lastLocation != null && lastLocation.distanceTo(location) < SAVE_PROGRESS_MIN_DISTANCE_METERS) {
            return
        }
        lastSavedProgress = destination to location
        synchronized(lock) {
            val current = session
            if (current != null && destinationState.value === destination) {
                val updated = current.copy(
                    progress = PathNavigationSessionProgress.of(guidance.distanceAlong, location)
                )
                session = updated
                store.save(updated)
            }
        }
    }

    /**
     * Restarts a navigation session, including its progress, that was previously cancelled.
     */
    fun restore(restored: PathNavigationSession) {
        synchronized(lock) {
            cancel()
            store.save(restored)
            buildJob = startBuild()
        }
    }

    /**
     * The session being followed, or the one still being built. Null if not navigating or the
     * saved session can't be read.
     */
    fun currentSession(): PathNavigationSession? {
        return synchronized(lock) {
            session ?: tryOrDefault<PathNavigationSession?>(null) { store.load() }
        }
    }

    fun cancel() {
        synchronized(lock) {
            buildJob?.cancel()
            restoringState.value = false
            session = null
            destinationState.value = null
            store.clear()
        }
    }

    /**
     * @return true if the path was being navigated and the navigation was canceled
     */
    fun cancel(pathId: Long): Boolean {
        if (destinationState.value?.path?.id != pathId) {
            return false
        }
        cancel()
        return true
    }

    suspend fun awaitRestore() {
        val job = synchronized(lock) { buildJob }
        job?.join()
    }

    private companion object {
        const val SAVE_PROGRESS_MIN_DISTANCE_METERS = 25f
    }
}
