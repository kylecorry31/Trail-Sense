package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.IReadingRepo
import com.kylecorry.trail_sense.weather.domain.CloudObservation
import java.time.Duration
import java.time.Instant

class CloudObservationRepo(private val context: Context) : IReadingRepo<CloudObservation> {

    private val readings = mutableListOf<Reading<CloudObservation>>()

    override suspend fun add(reading: Reading<CloudObservation>): Long {
        // TODO: Save in DB
        readings.add(reading)
        return readings.size.toLong()
    }

    override suspend fun delete(reading: Reading<CloudObservation>) {
        readings.removeIf { it.value.id == reading.value.id }
    }

    override suspend fun get(id: Long): Reading<CloudObservation>? {
        return readings.firstOrNull { it.value.id == id }
    }

    override suspend fun getAll(): List<Reading<CloudObservation>> {
        return readings
    }

    override fun getAllLive(): LiveData<List<Reading<CloudObservation>>> {
        TODO("Not yet implemented")
    }

    override suspend fun clean() {
        readings.removeAll { it.time < Instant.now().minus(Duration.ofDays(2)) }
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