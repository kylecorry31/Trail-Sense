package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.infrastructure.database.OldBeaconRepo
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import java.lang.Exception

class BeaconDatabaseMigrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val oldDb = OldBeaconRepo.getInstance(context)
            val oldBeacons = oldDb.get()
            val oldGroups = oldDb.getGroups()

            val beaconGroupEntities =
                oldGroups.map { BeaconGroupEntity.from(it) }

            val db = BeaconRepo.getInstance(context)

            val groupIdMap = mutableMapOf<Long, Long>()

            beaconGroupEntities.forEach {
                val oldId = it.id
                val newId = db.addBeaconGroup(it.also { it.id = 0 })
                groupIdMap[oldId] = newId
            }

            val beaconEntities = oldBeacons.map {
                BeaconEntity.from(it).copy(
                    beaconGroupId = when {
                        it.beaconGroupId == null -> {
                            null
                        }
                        groupIdMap.containsKey(it.beaconGroupId) -> {
                            groupIdMap[it.beaconGroupId]
                        }
                        else -> {
                            null
                        }
                    }
                ).also {
                    it.id = 0
                }
            }

            beaconEntities.forEach {
                db.addBeacon(it)
            }

            val cache = Cache(context)
            cache.remove(NavigatorFragment.LAST_BEACON_ID)


            context.deleteDatabase("survive")
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }

}