package com.kylecorry.trail_sense.tools.tides.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.core.units.Coordinate
import java.time.Instant
import java.time.ZonedDateTime

@Entity(tableName = "tides")
data class TideEntity(
    @ColumnInfo(name = "reference_high") val referenceHighTide: Long,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val reference: ZonedDateTime
        get() = Instant.ofEpochMilli(referenceHighTide).toZonedDateTime()

    val coordinate: Coordinate?
        get() {
            return if (latitude != null && longitude != null) {
                Coordinate(latitude, longitude)
            } else {
                null
            }
        }
}