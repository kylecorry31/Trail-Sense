package com.kylecorry.trail_sense.weather.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.topics.Topic
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.database.IReadingRepo
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import java.time.Duration
import java.time.Instant

class WeatherRepo private constructor(context: Context) : IReadingRepo<RawWeatherObservation> {

    private val pressureDao = AppDatabase.getInstance(context).pressureDao()

    private val _readingsChanged = Topic()
    val readingsChanged: ITopic = _readingsChanged

    override suspend fun add(reading: Reading<RawWeatherObservation>): Long = onIO {
        val entity = PressureReadingEntity.from(reading)

        val id = if (entity.id != 0L) {
            pressureDao.update(entity)
            entity.id
        } else {
            pressureDao.insert(entity)
        }
        _readingsChanged.publish()
        id
    }

    override suspend fun delete(reading: Reading<RawWeatherObservation>) = onIO {
        val entity = PressureReadingEntity.from(reading)
        pressureDao.delete(entity)
        _readingsChanged.publish()
    }

    override suspend fun get(id: Long): Reading<RawWeatherObservation>? = onIO {
        pressureDao.get(id)?.toWeatherObservation()
    }

    override suspend fun getAll(): List<Reading<RawWeatherObservation>> = onIO {
        pressureDao.getAllSync().map { it.toWeatherObservation() }
    }

    override fun getAllLive(): LiveData<List<Reading<RawWeatherObservation>>> {
        return pressureDao.getAll().map {
            it.map { reading -> reading.toWeatherObservation() }
        }
    }

    suspend fun getLast(): Reading<RawWeatherObservation>? = onIO {
        pressureDao.getLast()?.toWeatherObservation()
    }

    suspend fun addAll(value: List<Reading<RawWeatherObservation>>) = onIO {
        val entities = value.map { PressureReadingEntity.from(it) }
        val toAdd = entities.filter { it.id == 0L }
        val toUpdate = entities.filter { it.id != 0L }

        if (toAdd.isNotEmpty()) {
            pressureDao.bulkInsert(toAdd)
        }

        if (toUpdate.isNotEmpty()) {
            pressureDao.bulkUpdate(toUpdate)
        }

        _readingsChanged.publish()
    }

    override suspend fun clean() {
        pressureDao.deleteOlderThan(Instant.now().minus(PRESSURE_HISTORY_DURATION).toEpochMilli())

        // TODO: Only do this if there was a change
        _readingsChanged.publish()
    }

    companion object {

        private val PRESSURE_HISTORY_DURATION = Duration.ofDays(2).plusHours(6)

        private var instance: WeatherRepo? = null

        @Synchronized
        fun getInstance(context: Context): WeatherRepo {
            if (instance == null) {
                instance = WeatherRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}