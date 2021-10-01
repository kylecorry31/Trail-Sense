package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.database.IReadingRepo
import com.kylecorry.trail_sense.weather.domain.clouds.CloudObservation
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudReadingEntity
import java.time.Duration
import java.time.Instant

class CloudObservationRepo(context: Context) : IReadingRepo<CloudObservation> {

    private val dao = AppDatabase.getInstance(context).cloudDao()

    override suspend fun add(reading: Reading<CloudObservation>): Long {
        return if (reading.value.id == 0L) {
            dao.insert(CloudReadingEntity.fromReading(reading))
        } else {
            dao.update(CloudReadingEntity.fromReading(reading))
            reading.value.id
        }
    }

    override suspend fun delete(reading: Reading<CloudObservation>) {
        dao.delete(CloudReadingEntity.fromReading(reading))
    }

    override suspend fun get(id: Long): Reading<CloudObservation>? {
        return dao.get(id)?.toReading()
    }

    override suspend fun getAll(): List<Reading<CloudObservation>> {
        return dao.getAllSync().map { it.toReading() }
    }

    override fun getAllLive(): LiveData<List<Reading<CloudObservation>>> {
        return Transformations.map(dao.getAll()) {
            it.map { r -> r.toReading() }
        }
    }

    override suspend fun clean() {
        dao.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)).toEpochMilli())
    }

    companion object {
        private var instance: CloudObservationRepo? = null

        @Synchronized
        fun getInstance(context: Context): CloudObservationRepo {
            if (instance == null) {
                instance = CloudObservationRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}