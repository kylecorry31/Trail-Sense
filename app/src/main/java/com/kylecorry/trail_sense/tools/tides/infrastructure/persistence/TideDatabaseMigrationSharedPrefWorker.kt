package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import java.lang.Exception
import java.time.Instant

class TideDatabaseMigrationSharedPrefWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val cache = Cache(context)
            val oldReference = cache.getLong(context.getString(R.string.reference_high_tide)) ?: return Result.success()

            val oldInstant = Instant.ofEpochSecond(oldReference)
            val db = TideRepo.getInstance(context)

            db.addTide(TideEntity(oldInstant.toEpochMilli(), null, null, null))

            cache.remove(context.getString(R.string.reference_high_tide))
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }

}