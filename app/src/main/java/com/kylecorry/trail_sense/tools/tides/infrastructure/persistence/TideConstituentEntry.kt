package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.trail_sense.shared.data.Identifiable

@Entity(tableName = "tide_constituents")
data class TideConstituentEntry(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") override val id: Long,
    @ColumnInfo(name = "table_id") val tableId: Long,
    @ColumnInfo(name = "constituent_id") val constituentId: Long,
    @ColumnInfo(name = "amplitude") val amplitude: Float,
    @ColumnInfo(name = "phase") val phase: Float
) : Identifiable {

    fun toHarmonic(): TidalHarmonic {
        return TidalHarmonic(
            TideConstituent.entries.find { it.id == constituentId } ?: TideConstituent.M2,
            amplitude,
            phase
        )
    }

    companion object {
        fun from(id: Long, tableId: Long, harmonic: TidalHarmonic): TideConstituentEntry {
            return TideConstituentEntry(
                id,
                tableId,
                harmonic.constituent.id,
                harmonic.amplitude,
                harmonic.phase
            )
        }
    }

}
