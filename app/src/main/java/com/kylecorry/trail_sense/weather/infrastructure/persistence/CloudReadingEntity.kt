package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.clouds.CloudObservation
import java.time.Instant

@Entity(tableName = "clouds")
data class CloudReadingEntity(
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "cover") val cover: Float
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0


    fun toReading(): Reading<CloudObservation> {
        return Reading(CloudObservation(id, cover), time)
    }

    companion object {
        fun fromReading(reading: Reading<CloudObservation>): CloudReadingEntity {
            return CloudReadingEntity(reading.time, reading.value.coverage).also {
                it.id = reading.value.id
            }
        }
    }

}