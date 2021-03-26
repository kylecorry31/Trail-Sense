package com.kylecorry.trail_sense.weather.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.weather.domain.PressureReadingEntity
import com.kylecorry.trail_sense.weather.infrastructure.legacydatabase.OldPressureRepo
import java.lang.Exception
import java.time.Duration
import java.time.Instant

class PressureDatabaseMigrationWorker(private val context: Context,
                                      workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val oldDb = OldPressureRepo.getInstance(context)
            val oldReadings = oldDb.get()
            oldDb.deleteAll()

            val entities = oldReadings.map {
                PressureReadingEntity(it.pressure, it.altitude, 0f, it.temperature, 0f, it.time.toEpochMilli())
            }
            val db = PressureRepo.getInstance(context)
            entities.forEach {
                db.addPressure(it)
            }
            db.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))

            context.deleteDatabase("weather")
            context.deleteFile("pressure.csv")
        } catch (e: Exception){
            // Do nothing - could not migrate DB, so user will lose their pressure history
        }
        return Result.success()
    }

}