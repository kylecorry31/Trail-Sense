package com.kylecorry.trail_sense.tools.battery.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.Instant

@Entity(tableName = "battery", indices = [Index(value = ["time"])])
class BatteryReadingEntity(
    @ColumnInfo(name = "percent") val percent: Float,
    @ColumnInfo(name = "capacity") val capacity: Float,
    @ColumnInfo(name = "isCharging") val isCharging: Boolean,
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "uptime") val uptime: Long? = null,
    @ColumnInfo(name = "elapsed_realtime") val elapsedRealtime: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toBatteryReading(): BatteryReading {
        return BatteryReading(
            time,
            percent,
            capacity,
            isCharging,
            uptime?.let { Duration.ofMillis(it) },
            elapsedRealtime?.let { Duration.ofMillis(it) }
        )
    }

    companion object {
        fun from(reading: BatteryReading): BatteryReadingEntity {
            return BatteryReadingEntity(
                reading.percent,
                reading.capacity,
                reading.isCharging,
                reading.time,
                reading.uptime?.toMillis(),
                reading.elapsedRealtime?.toMillis()
            )
        }
    }

}
