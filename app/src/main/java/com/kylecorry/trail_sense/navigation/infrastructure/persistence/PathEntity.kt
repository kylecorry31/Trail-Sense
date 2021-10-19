package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.database.Identifiable
import com.kylecorry.trail_sense.shared.paths.*
import java.time.Instant

@Entity(tableName = "paths")
data class PathEntity(
    @ColumnInfo(name = "name") val name: String?,
    // Style
    @ColumnInfo(name = "lineStyle") val lineStyle: LineStyle,
    @ColumnInfo(name = "pointStyle") val pointStyle: PathPointColoringStyle,
    @ColumnInfo(name = "color") val color: AppColor,
    @ColumnInfo(name = "visible") val visible: Boolean,
    // Saved
    @ColumnInfo(name = "temporary") val temporary: Boolean = false,
    // Metadata
    @ColumnInfo(name = "distance") val distance: Float,
    @ColumnInfo(name = "numWaypoints") val numWaypoints: Int,
    @ColumnInfo(name = "startTime") val startTime: Long?,
    @ColumnInfo(name = "endTime") val endTime: Long?,
    // Bounds
    @ColumnInfo(name = "north") val north: Double,
    @ColumnInfo(name = "east") val east: Double,
    @ColumnInfo(name = "south") val south: Double,
    @ColumnInfo(name = "west") val west: Double,
) : Identifiable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    override var id: Long = 0L

    fun toPath(): Path2 {
        return Path2(
            id,
            name,
            PathStyle(
                lineStyle,
                pointStyle,
                color.color,
                visible
            ),
            PathMetadata(
                Distance.meters(distance),
                numWaypoints,
                if (startTime != null && endTime != null) Range(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)) else null,
                CoordinateBounds(north, east, south, west)
            ),
            temporary
        )
    }

    companion object {
        fun from(path: Path2): PathEntity {
            return PathEntity(
                path.name,
                path.style.line,
                path.style.point,
                AppColor.values().firstOrNull { it.color == path.style.color } ?: AppColor.Gray,
                path.style.visible,
                path.temporary,
                path.metadata.distance.meters().distance,
                path.metadata.waypoints,
                path.metadata.duration?.start?.toEpochMilli(),
                path.metadata.duration?.end?.toEpochMilli(),
                path.metadata.bounds.north,
                path.metadata.bounds.east,
                path.metadata.bounds.south,
                path.metadata.bounds.west
            ).also {
                it.id = path.id
            }
        }
    }


}