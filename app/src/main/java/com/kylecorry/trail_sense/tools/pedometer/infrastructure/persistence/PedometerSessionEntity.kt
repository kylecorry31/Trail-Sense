package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.pedometer.domain.PedometerSession
import java.time.Instant

// #1397: Room entity for storing completed pedometer sessions (one per reset cycle)
@Entity(
    tableName = "pedometer_sessions",
    indices = [Index(value = ["start_time"])]
)
data class PedometerSessionEntity(
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long,
    @ColumnInfo(name = "steps") val steps: Long,
    @ColumnInfo(name = "distance") val distance: Float
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toPedometerSession(): PedometerSession {
        return PedometerSession(
            id,
            Instant.ofEpochMilli(startTime),
            Instant.ofEpochMilli(endTime),
            steps,
            distance
        )
    }

    companion object {
        fun from(session: PedometerSession): PedometerSessionEntity {
            return PedometerSessionEntity(
                session.startTime.toEpochMilli(),
                session.endTime.toEpochMilli(),
                session.steps,
                session.distance
            ).also {
                it.id = session.id
            }
        }
    }
}
