package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.PathNavigationMode
import com.kylecorry.trail_sense.tools.navigation.domain.PathRouteBuilder
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PathNavigator(context: Context) {
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()
    private val store = PathNavigationStore(context)
    private val destinationState = MutableStateFlow<Destination.Path?>(null)
    private val restoringState = MutableStateFlow(store.hasSavedRoute())
    val isRestoring = restoringState.asStateFlow()
    private val restoreLock = Any()
    private var restoreCancelled = false
    private val restoreTask = CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
        try {
            val restored = store.restore()
            synchronized(restoreLock) {
                if (!restoreCancelled) destinationState.value = restored
            }
        } finally {
            restoringState.value = false
        }
    }

    val destination: Flow<Destination.Path?> = destinationState

    init {
        restoreTask.start()
        Tools.subscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED) {
            if (destinationState.value != null) {
                onIO {
                    destinationState.value?.route?.navigate(locationSubsystem.location)
                }
            }
        }
    }

    suspend fun navigate(
        path: Path,
        points: List<PathPoint>,
        mode: PathNavigationMode = PathNavigationMode.TO_END,
        destinationPointId: Long? = null
    ) {
        if (points.isEmpty()) return
        if (destinationPointId != null && points.none { it.id == destinationPointId }) return
        val start = locationSubsystem.location
        val route = PathRouteBuilder.prepare(points, start, mode, destinationPointId)
        cancel()
        destinationState.value = onIO { store.save(path, route, start, mode, destinationPointId) }
    }

    fun cancel() {
        synchronized(restoreLock) {
            restoreCancelled = true
            restoreTask.cancel()
            restoringState.value = false
            store.clear()
            destinationState.value = null
        }
    }

    fun isNavigating(): Boolean = destinationState.value != null

    suspend fun awaitRestore() {
        restoreTask.join()
    }
}
