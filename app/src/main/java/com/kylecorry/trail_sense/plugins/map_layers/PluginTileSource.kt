package com.kylecorry.trail_sense.plugins.map_layers

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import java.io.ByteArrayInputStream
import java.time.Instant

class PluginTileSource(private val packageId: String, private val endpoint: String) : TileSource {

    private val plugins = getAppService<PluginSubsystem>()

    override suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle
    ): Bitmap? {
        val connection = plugins.getPluginResourceServiceConnection(packageId)
        return connection?.use {
            val payload = Payload(
                tile.x,
                tile.y,
                tile.z,
                params.getLong(MapLayerParams.PARAM_TIME, Instant.now().toEpochMilli())
            )
            val bitmap =
                it.send(
                    endpoint,
                    payload,
                    requiredPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
                )?.payload
                    ?: return@use null
            BitmapFactory.decodeStream(ByteArrayInputStream(bitmap))
        }
    }

    private data class Payload(
        val x: Int,
        val y: Int,
        val z: Int,
        val time: Long
    ) : ProguardIgnore
}
