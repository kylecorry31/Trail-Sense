package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.IEventSubscriber
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathGuidance
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSession
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSessionProgress
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRouteBuilder
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolEventSubscriber
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

class PathNavigator(
    private val prefs: IPathPreferences,
    private val store: PathNavigationStore,
    private val paths: IPathService,
    private val currentLocation: () -> Coordinate,
    private val currentAccuracy: () -> Distance?,
    eventSubscriber: IEventSubscriber
) {
    private val destinationState = MutableStateFlow<Destination.Path?>(null)
    private val restoringState = MutableStateFlow(false)
    val isRestoring = restoringState.asStateFlow()

    private val lock = Any()
    private var session: PathNavigationSession? = null
    private var buildJob: Job? = null
    private var lastSavedProgress: Pair<Destination.Path, Coordinate>? = null

    val destination: Flow<Destination.Path?> = destinationState

    constructor(context: Context) : this(
        getAppService<UserPreferences>().paths,
        PathNavigationStore(),
        PathService.getInstance(context),
        DependencyRegistry.get<LocationSubsystem>().let { subsystem -> { subsystem.location } },
        DependencyRegistry.get<LocationSubsystem>().let { subsystem -> { subsystem.locationAccuracy } },
        ToolEventSubscriber
    )

    init {
        if (store.hasSavedRoute()) {
            synchronized(lock) {
                buildJob = startBuild()
            }
        }
        eventSubscriber.subscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED) {
            destinationState.value?.let { destination ->
                tryOrLog {
                    val location = currentLocation()
                    val guidance = destination.route.navigate(location, currentAccuracy())
                    saveProgress(destination, guidance, location)
                }
            }
        }
    }

    fun navigate(path: Path, follow: PathFollow) {
        restore(PathNavigationSession(path.id, follow, currentLocation()))
    }

    private fun startBuild(): Job {
        restoringState.value = true
        return CoroutineScope(Dispatchers.Default).launch {
            try {
                val built = try {
                    build()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
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
                    if (buildJob === coroutineContext.job) {
                        restoringState.value = false
                    }
                }
            }
        }
    }

    private suspend fun build(): Pair<PathNavigationSession, Destination.Path>? {
        val session = store.load() ?: return null
        val path = paths.getPath(session.pathId) ?: return null
        val points = HikingService.correctElevations(
            paths.getWaypoints(session.pathId).sortedBy { it.id },
            prefs.elevationSmoothing
        )
        currentCoroutineContext().ensureActive()
        val route = PathRouteBuilder.buildRoute(points, session.start, session.follow) ?: return null
        val destination = Destination.Path(path, route)
        session.progress?.let {
            destination.route.restorePosition(it.distanceAlong, it.location)
        }
        destination.route.navigate(currentLocation(), currentAccuracy())
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
                    progress = PathNavigationSessionProgress(guidance.distanceAlong, location)
                )
                session = updated
                store.save(updated)
            }
        }
    }

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
     * @return true if the path was being navigated (or its route was being built) and the navigation was canceled
     */
    fun cancel(pathId: Long): Boolean {
        synchronized(lock) {
            if (currentSession()?.pathId != pathId) {
                return false
            }
            cancel()
            return true
        }
    }

    suspend fun awaitRestore() {
        val job = synchronized(lock) { buildJob }
        job?.join()
    }

    private companion object {
        const val SAVE_PROGRESS_MIN_DISTANCE_METERS = 25f
    }
}
