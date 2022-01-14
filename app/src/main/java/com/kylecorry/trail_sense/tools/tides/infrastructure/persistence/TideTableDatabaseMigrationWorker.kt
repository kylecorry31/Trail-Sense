package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class TideTableDatabaseMigrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val prefs = UserPreferences(context)
            prefs.tides.lastTide = null

            val oldDb = OldTideRepo(context)
            val oldTides = oldDb.getTides()

            val tables = oldTides.map {
                TideTable(0, listOf(Tide.high(it.reference)), it.name, it.coordinate)
            }

            val db = TideTableRepo.getInstance(context)
            for (table in tables) {
                db.addTideTable(table)
            }

            oldDb.deleteAll()
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }

}