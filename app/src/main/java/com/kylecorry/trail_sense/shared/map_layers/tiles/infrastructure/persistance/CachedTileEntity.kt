package com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import java.time.Instant

@Entity(
    tableName = "cached_tiles",
    indices = [
        Index(value = ["key", "x", "y", "z"]),
        Index(value = ["last_used_on"])
    ]
)
data class CachedTileEntity(
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "x") val x: Int,
    @ColumnInfo(name = "y") val y: Int,
    @ColumnInfo(name = "z") val z: Int,
    @ColumnInfo(name = "filename") val filename: String,
    @ColumnInfo(name = "created_on") val createdOn: Instant,
    @ColumnInfo(name = "last_used_on") val lastUsedOn: Instant,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "has_alpha") val hasAlpha: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toCachedTile(): CachedTile {
        return CachedTile(
            id = id,
            key = key,
            tile = Tile(x, y, z),
            filename = filename,
            createdOn = createdOn,
            lastUsedOn = lastUsedOn,
            sizeBytes = sizeBytes,
            hasAlpha = hasAlpha
        )
    }

    companion object {
        fun from(cachedTile: CachedTile): CachedTileEntity {
            return CachedTileEntity(
                key = cachedTile.key,
                x = cachedTile.tile.x,
                y = cachedTile.tile.y,
                z = cachedTile.tile.z,
                filename = cachedTile.filename,
                createdOn = cachedTile.createdOn,
                lastUsedOn = cachedTile.lastUsedOn,
                sizeBytes = cachedTile.sizeBytes,
                hasAlpha = cachedTile.hasAlpha
            ).also {
                it.id = cachedTile.id
            }
        }
    }
}
