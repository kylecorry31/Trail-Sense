package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import java.time.Instant

@Entity(tableName = "field_guide_sightings")
data class FieldGuideSightingEntity(
    @ColumnInfo(name = "field_guide_page_id") val fieldGuidePageId: Long,
    @ColumnInfo(name = "time") val time: Instant? = null,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "altitude") val altitude: Float? = null,
    @ColumnInfo(name = "harvested") val harvested: Boolean? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toSighting(): Sighting {
        return Sighting(
            id,
            fieldGuidePageId,
            time,
            if (latitude != null && longitude != null) {
                Coordinate(latitude, longitude)
            } else {
                null
            },
            altitude,
            harvested,
            notes
        )
    }

    companion object {
        fun fromSighting(sighting: Sighting): FieldGuideSightingEntity {
            return FieldGuideSightingEntity(
                sighting.fieldGuidePageId,
                sighting.time,
                sighting.location?.latitude,
                sighting.location?.longitude,
                sighting.altitude,
                sighting.harvested,
                sighting.notes
            ).apply {
                id = sighting.id
            }
        }
    }
}