package com.kylecorry.trail_sense.plugins.map_layers

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.infrastructure.PluginGuard
import com.kylecorry.trail_sense.plugins.infrastructure.PluginResourceServiceConnection
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.time.Instant

class PluginTileSource(private val packageId: String, private val endpoint: String) : TileSource {

    private val plugins = getAppService<PluginSubsystem>()
    private var connection: PluginResourceServiceConnection? = null
    private val connectionLock = Mutex()

    override suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle
    ): Bitmap? {
        val payload = Payload(
            tile.x,
            tile.y,
            tile.z,
            params.getLong(MapLayerParams.PARAM_TIME, Instant.now().toEpochMilli())
        )
        val imageBytes = trySend(payload)
        return imageBytes?.let { decodeBitmap(it) }
    }

    private suspend fun trySend(payload: Payload): ByteArray? {
        val connection = getOrCreateConnection()
        val response = connection?.send(
            endpoint,
            payload,
            requiredPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        )

        return if (response == null) {
            cleanup()
            null
        } else {
            response.payload
        }
    }

    private suspend fun getOrCreateConnection(): PluginResourceServiceConnection? {
        return connectionLock.withLock {
            if (connection == null) {
                connection = plugins.getPluginResourceServiceConnection(packageId)
            }
            connection
        }
    }

    override suspend fun cleanup() {
        connectionLock.withLock {
            connection?.close()
            connection = null
        }
    }

    private fun decodeBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val isValid = PluginGuard.isValidTilePayload(bytes) &&
                BitmapFactory.decodeStream(ByteArrayInputStream(bytes), null, bounds) == null &&
                PluginGuard.isValidTileSize(bounds.outWidth, bounds.outHeight)

        return if (isValid) {
            BitmapFactory.decodeStream(ByteArrayInputStream(bytes))
        } else {
            null
        }
    }

    private data class Payload(
        val x: Int,
        val y: Int,
        val z: Int,
        val time: Long
    ) : ProguardIgnore
}
