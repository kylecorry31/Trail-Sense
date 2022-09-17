package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import java.time.Instant

@Entity(tableName = "clouds")
data class CloudReadingEntity(
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "genus") val genus: CloudGenus?
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toReading(): Reading<CloudObservation> {
        return Reading(CloudObservation(id, genus), time)
    }

    companion object {
        fun from(reading: Reading<CloudObservation>): CloudReadingEntity {
            return CloudReadingEntity(reading.time, reading.value.genus).also {
                it.id = reading.value.id
            }
        }
    }

}