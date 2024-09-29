package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import java.time.Duration

@Entity(tableName = "tide_tables")
data class TideTableEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") override val id: Long,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "isSemidiurnal") val isSemidiurnal: Boolean,
    @ColumnInfo(name = "isVisible") val isVisible: Boolean,
    @ColumnInfo(name = "estimateType") val estimateType: Long,
    @ColumnInfo(name = "lunitidalInterval") val lunitidalInterval: Duration? = null,
    @ColumnInfo(name = "lunitidalIntervalIsUtc") val lunitidalIntervalIsUtc: Boolean = true
) : Identifiable {

    fun toTable(tides: List<Tide>, harmonics: List<TidalHarmonic> = emptyList()): TideTable {
        val coordinate = if (latitude != null && longitude != null) {
            Coordinate(latitude, longitude)
        } else {
            null
        }
        return TideTable(
            id,
            tides,
            name,
            coordinate,
            isSemidiurnal,
            isVisible,
            TideEstimator.entries.withId(estimateType) ?: TideEstimator.Clock,
            harmonics,
            lunitidalInterval,
            lunitidalIntervalIsUtc
        )
    }

    companion object {
        fun from(table: TideTable): TideTableEntity {
            return TideTableEntity(
                table.id,
                table.name,
                table.location?.latitude,
                table.location?.longitude,
                table.isSemidiurnal,
                table.isVisible,
                table.estimator.id,
                table.lunitidalInterval,
                table.lunitidalIntervalIsUtc
            )
        }
    }

}
