package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.database.Identifiable
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

@Entity(tableName = "tide_tables")
data class TideTableEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") override val id: Long,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "isSemidiurnal") val isSemidiurnal: Boolean,
    @ColumnInfo(name = "isVisible") val isVisible: Boolean
) : Identifiable {

    fun toTable(tides: List<Tide>): TideTable {
        val coordinate = if (latitude != null && longitude != null) {
            Coordinate(latitude, longitude)
        } else {
            null
        }
        return TideTable(id, tides, name, coordinate, isSemidiurnal, isVisible)
    }

    companion object {
        fun from(table: TideTable): TideTableEntity {
            return TideTableEntity(
                table.id,
                table.name,
                table.location?.latitude,
                table.location?.longitude,
                table.isSemidiurnal,
                table.isVisible
            )
        }
    }

}
