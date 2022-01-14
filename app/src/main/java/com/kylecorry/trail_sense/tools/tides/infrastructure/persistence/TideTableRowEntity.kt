package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.shared.database.Identifiable
import java.time.Instant
import java.time.ZoneId

@Entity(tableName = "tide_table_rows")
data class TideTableRowEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") override val id: Long,
    @ColumnInfo(name = "table_id") val tableId: Long,
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "high") val isHigh: Boolean,
    @ColumnInfo(name = "height") val heightMeters: Float?
) : Identifiable {

    fun toTide(): Tide {
        return Tide(
            time.atZone(ZoneId.systemDefault()),
            isHigh,
            heightMeters
        )
    }

    companion object {
        fun from(id: Long, tableId: Long, tide: Tide): TideTableRowEntity {
            return TideTableRowEntity(
                id,
                tableId,
                tide.time.toInstant(),
                tide.isHigh,
                tide.height
            )
        }
    }

}
