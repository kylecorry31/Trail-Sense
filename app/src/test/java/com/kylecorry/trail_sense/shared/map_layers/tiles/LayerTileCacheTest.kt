package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.UUID

internal class LayerTileCacheTest {

    @Test
    fun movesLoadedTilesBetweenLayerAndSharedCaches() {
        val owner = UUID.randomUUID().toString()
        val cache = LayerTileCache(owner, 1)
        val firstTile = Tile(0, 0, 1)
        val secondTile = Tile(1, 0, 1)
        val first = cache.getOrPut(firstTile) { key -> loadedTile(key, firstTile, owner) }

        cache.getOrPut(secondTile) { key -> loadedTile(key, secondTile, owner) }

        assertSame(first, cache.peek(firstTile))
        assertSame(first, cache.getOrPut(firstTile) { key -> loadedTile(key, firstTile, owner) })
        cache.clear()
    }

    @Test
    fun removesSharedTileWhenLoadCompletesWithoutImage() = runBlocking {
        val owner = UUID.randomUUID().toString()
        val cache = LayerTileCache(owner, 1)
        val firstTile = Tile(0, 0, 1)
        val secondTile = Tile(1, 0, 1)
        val first = cache.getOrPut(firstTile) { key ->
            ImageTile(
                key = key,
                tile = firstTile,
                state = TileState.Loading,
                owner = owner,
                loadFunction = { null }
            )
        }
        cache.getOrPut(secondTile) { key -> loadedTile(key, secondTile, owner) }

        first.load()
        cache.onLoadComplete(first)

        val replacement = cache.getOrPut(firstTile) { key -> loadedTile(key, firstTile, owner) }
        assertNotSame(first, replacement)
        cache.clear()
    }

    @Test
    fun invalidatesTilesInBothCaches() {
        val owner = UUID.randomUUID().toString()
        val cache = LayerTileCache(owner, 1)
        val firstTile = Tile(0, 0, 1)
        val secondTile = Tile(1, 0, 1)
        val first = cache.getOrPut(firstTile) { key -> loadedTile(key, firstTile, owner) }
        val second = cache.getOrPut(secondTile) { key -> loadedTile(key, secondTile, owner) }

        cache.invalidate()

        assertEquals(TileState.Stale, first.state)
        assertEquals(TileState.Stale, second.state)
        cache.clear()
    }

    private fun loadedTile(key: String, tile: Tile, owner: String): ImageTile {
        return ImageTile(
            key = key,
            tile = tile,
            image = mock<Bitmap>(),
            state = TileState.Loaded,
            owner = owner,
            loadFunction = null
        )
    }
}
