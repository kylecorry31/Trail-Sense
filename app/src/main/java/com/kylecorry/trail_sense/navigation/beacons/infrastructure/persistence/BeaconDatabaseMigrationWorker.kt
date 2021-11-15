package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment

class BeaconDatabaseMigrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val cache = Preferences(context)
            cache.remove(NavigatorFragment.LAST_BEACON_ID)
            context.deleteDatabase("survive")
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }

}