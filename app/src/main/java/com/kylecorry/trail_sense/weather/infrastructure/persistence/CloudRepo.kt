package com.kylecorry.trail_sense.weather.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.topics.Topic
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.database.IReadingRepo
import com.kylecorry.trail_sense.shared.extensions.onIO
import java.time.Duration
import java.time.Instant

class CloudRepo private constructor(private val dao: CloudReadingDao) :
    IReadingRepo<CloudObservation> {

    private val _readingsChanged = Topic()
    val readingsChanged: ITopic = _readingsChanged

    override suspend fun clean() = onIO {
        dao.deleteOlderThan(Instant.now().minus(CLOUD_HISTORY_DURATION).toEpochMilli())

        // TODO: Only do this if there was a change
        _readingsChanged.publish()
    }

    override suspend fun add(reading: Reading<CloudObservation>): Long = onIO {
        val entity = CloudReadingEntity.from(reading)

        val id = if (entity.id != 0L) {
            dao.update(entity)
            entity.id
        } else {
            dao.insert(entity)
        }
        _readingsChanged.publish()
        id
    }

    override suspend fun delete(reading: Reading<CloudObservation>) = onIO {
        val entity = CloudReadingEntity.from(reading)
        dao.delete(entity)
        _readingsChanged.publish()
    }

    override suspend fun get(id: Long): Reading<CloudObservation>? = onIO {
        dao.get(id)?.toReading()
    }

    override suspend fun getAll(): List<Reading<CloudObservation>> = onIO {
        dao.getAllSync().map { it.toReading() }
    }

    override fun getAllLive(): LiveData<List<Reading<CloudObservation>>> {
        return Transformations.map(dao.getAll()) {
            it.map { it.toReading() }
        }
    }

    companion object {

        private val CLOUD_HISTORY_DURATION = Duration.ofDays(2)

        private var instance: CloudRepo? = null

        @Synchronized
        fun getInstance(context: Context): CloudRepo {
            if (instance == null) {
                instance = CloudRepo(AppDatabase.getInstance(context).cloudDao())
            }
            return instance!!
        }
    }
}