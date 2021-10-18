package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.paths.Path2
import com.kylecorry.trail_sense.shared.paths.PathMetadata
import com.kylecorry.trail_sense.shared.paths.PathStyle

class PathDatabaseMigrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val prefs = NavigationPreferences(context)
            val pathService = PathService.getInstance(context)
            val paths = pathService.getWaypoints()

            val style = PathStyle(
                prefs.backtrackPathStyle,
                prefs.backtrackPointStyle,
                prefs.backtrackPathColor.color,
                true
            )

            pathService.endBacktrackPath()

            for (path in paths) {
                val newPath = pathService.addPath(Path2(0, null, style, PathMetadata.empty))
                pathService.moveWaypointsToPath(path.value.map { it.copy(pathId = 0) }, newPath)
            }
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }

}