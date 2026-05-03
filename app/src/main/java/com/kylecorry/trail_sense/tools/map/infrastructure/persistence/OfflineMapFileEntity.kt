package com.kylecorry.trail_sense.tools.map.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileType
import java.time.Instant

@Entity(tableName = "offline_map_files")
data class OfflineMapFileEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: Long,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "created_on") val createdOn: Long,
    @ColumnInfo(name = "north") val north: Double?,
    @ColumnInfo(name = "east") val east: Double?,
    @ColumnInfo(name = "south") val south: Double?,
    @ColumnInfo(name = "west") val west: Double?,
    @ColumnInfo(name = "visible") val visible: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toOfflineMapFile(): OfflineMapFile {
        val hasLatitudeBounds = north != null && south != null
        val hasLongitudeBounds = east != null && west != null
        return OfflineMapFile(
            id,
            name,
            OfflineMapFileType.entries.firstOrNull { it.id == type }
                ?: OfflineMapFileType.Mapsforge,
            path,
            sizeBytes,
            Instant.ofEpochMilli(createdOn),
            if (hasLatitudeBounds && hasLongitudeBounds) {
                CoordinateBounds(north, east, south, west)
            } else {
                null
            },
            visible
        )
    }

    companion object {
        fun from(file: OfflineMapFile): OfflineMapFileEntity {
            return OfflineMapFileEntity(
                file.name,
                file.type.id,
                file.path,
                file.sizeBytes,
                file.createdOn.toEpochMilli(),
                file.bounds?.north,
                file.bounds?.east,
                file.bounds?.south,
                file.bounds?.west,
                file.visible
            ).also {
                it.id = file.id
            }
        }
    }
}
