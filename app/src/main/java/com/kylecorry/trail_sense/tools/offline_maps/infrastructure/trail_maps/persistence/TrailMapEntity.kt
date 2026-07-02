package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import java.time.Instant

@Entity(tableName = "offline_map_files", indices = [Index(value = ["parent"])])
data class TrailMapEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: Long,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "created_on") val createdOn: Long,
    @ColumnInfo(name = "north") val north: Double?,
    @ColumnInfo(name = "east") val east: Double?,
    @ColumnInfo(name = "south") val south: Double?,
    @ColumnInfo(name = "west") val west: Double?,
    @ColumnInfo(name = "attribution") val attribution: String?,
    @ColumnInfo(name = "visible") val visible: Boolean,
    @ColumnInfo(name = "parent") val parent: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toTrailMap(): TrailMap {
        val hasLatitudeBounds = north != null && south != null
        val hasLongitudeBounds = east != null && west != null
        return TrailMap(
            id,
            name,
            listOf(
                OfflineMapFile(path, sizeBytes, TrailMap.FILE_ROLE_MAPSFORGE_MAP)
            ),
            Instant.ofEpochMilli(createdOn),
            if (hasLatitudeBounds && hasLongitudeBounds) {
                CoordinateBounds(north, east, south, west)
            } else {
                null
            },
            attribution,
            visible,
            parent
        )
    }

    companion object {
        private const val LEGACY_TYPE_ID = 1L

        fun from(file: TrailMap): TrailMapEntity {
            return TrailMapEntity(
                file.name,
                LEGACY_TYPE_ID,
                file.mapFile.path,
                file.mapFile.sizeBytes,
                file.createdOn.toEpochMilli(),
                file.bounds?.north,
                file.bounds?.east,
                file.bounds?.south,
                file.bounds?.west,
                file.attribution,
                file.visible,
                file.parentId
            ).also {
                it.id = file.id
            }
        }
    }
}
