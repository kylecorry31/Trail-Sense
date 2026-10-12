package com.kylecorry.trail_sense.tools.paths.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PathElevationPrefetcher {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val requested = mutableSetOf<Coordinate>()
    private var elevations = ConcurrentHashMap<Coordinate, Float>()

    @Suppress("TooGenericExceptionCaught")
    fun update(points: List<Coordinate>) {
        if (points.isEmpty()) {
            scope.coroutineContext.cancelChildren()
            requested.clear()
            // Lookups that were already running keep writing to the old map
            elevations = ConcurrentHashMap()
            return
        }

        val newPoints = points.filter { isValid(it) && requested.add(it) }
        if (newPoints.isEmpty()) {
            return
        }

        val target = elevations
        scope.launch {
            try {
                newPoints.forEach { point ->
                    val elevation = DEM.getElevation(point)
                    if (elevation.hasData) {
                        target[point] = elevation.elevation
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                getAppService<Logger>().error(TAG, "Unable to prefetch path elevations", e)
            }
        }
    }

    fun getElevations(): Map<Coordinate, Float> {
        return elevations.toMap()
    }

    companion object {
        private const val TAG = "PathElevationPrefetcher"

        fun isValid(location: Coordinate): Boolean {
            return !location.latitude.isNaN() && !location.longitude.isNaN()
        }
    }
}
