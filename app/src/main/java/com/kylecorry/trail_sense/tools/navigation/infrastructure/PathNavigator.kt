package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRouteBuilder
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PathNavigator(context: Context) {
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()
    private val store = PathNavigationStore()
    private val paths = PathService.getInstance(context)
    private val destinationState = MutableStateFlow<Destination.Path?>(null)
    private val restoringState = MutableStateFlow(store.hasSavedRoute())
    val isRestoring = restoringState.asStateFlow()
    private val restoreLock = Any()
    private var restoreCancelled = false
    private val restoreTask = CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
        try {
            val restored = restore()
            synchronized(restoreLock) {
                if (!restoreCancelled) destinationState.value = restored
            }
        } finally {
            restoringState.value = false
        }
    }

    val destination: Flow<Destination.Path?> = destinationState

    private suspend fun restore(): Destination.Path? {
        return try {
            val session = store.load() ?: return null
            val path = requireNotNull(paths.getPath(session.pathId))
            val points = paths.getWaypoints(session.pathId)
            require(points.isNotEmpty())
            currentCoroutineContext().ensureActive()
            val route = PathRouteBuilder.buildRoute(
                points, session.start, session.reversed, session.destinationPointId
            )
            val destination = Destination.Path(path, route)
            session.progress?.let {
                if (destination.route.length > 0f) {
                    destination.route.restoreProgress(it.distance / destination.route.length, it.location)
                }
            }
            destination
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            synchronized(restoreLock) {
                if (!restoreCancelled) store.clear()
            }
            null
        }
    }

    init {
        restoreTask.start()
        Tools.subscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED) {
            val destination = destinationState.value
            if (destination != null) {
                tryOrLog {
                    val location = locationSubsystem.location
                    val guidance = destination.route.navigate(location, locationSubsystem.locationAccuracy)
                    store.saveProgress(guidance.progress * destination.route.length, location) {
                        destinationState.value === destination
                    }
                }
            }
        }
    }

    suspend fun navigate(
        path: Path,
        points: List<PathPoint>,
        reversed: Boolean = false,
        destinationPointId: Long? = null
    ) {
        if (points.isEmpty()) return
        if (destinationPointId != null && points.none { it.id == destinationPointId }) return
        val start = locationSubsystem.location
        val route = PathRouteBuilder.buildRoute(points, start, reversed, destinationPointId)
        cancel()
        store.save(path, start, reversed, destinationPointId)
        destinationState.value = Destination.Path(path, route)
    }

    fun cancel() {
        synchronized(restoreLock) {
            restoreCancelled = true
            restoreTask.cancel()
            restoringState.value = false
            destinationState.value = null
            store.clear()
        }
    }

    fun cancel(pathId: Long) {
        val navigatedPathId = destinationState.value?.path?.id ?: store.getSavedPathId()
        if (navigatedPathId == pathId) {
            cancel()
        }
    }

    fun isNavigating(): Boolean = destinationState.value != null

    suspend fun awaitRestore() {
        restoreTask.join()
    }
}
