package com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance

import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import java.time.Instant

class CachedTile(
    val id: Long,
    val key: String,
    val tile: Tile,
    val filename: String,
    val createdOn: Instant,
    val lastUsedOn: Instant,
    val sizeBytes: Long,
    val hasAlpha: Boolean
)