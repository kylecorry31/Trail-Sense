package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun keepsSharedTileWhenLoadCompletesWithoutImage() = runBlocking {
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

        try {
            assertEquals(TileState.Empty, first.state)
            assertSame(
                first,
                cache.getOrPut(firstTile) { error("Empty tile was loaded again") }
            )
        } finally {
            cache.clear()
        }
    }

    @Test
    fun clearingLayerDoesNotEvictOtherOwnersFromSharedCache() {
        val otherOwner = UUID.randomUUID().toString()
        val clearedOwner = UUID.randomUUID().toString()
        val otherCache = LayerTileCache(otherOwner, 1)
        val clearedCache = LayerTileCache(clearedOwner, 1)
        val sharedTile = Tile(0, 0, 9)
        val shared = otherCache.getOrPut(sharedTile) { key ->
            loadedTile(key, sharedTile, otherOwner)
        }
        repeat(256) { x ->
            val tile = Tile(x + 1, 0, 9)
            otherCache.getOrPut(tile) { key -> loadedTile(key, tile, otherOwner) }
        }
        val clearedTile = Tile(0, 1, 9)
        clearedCache.getOrPut(clearedTile) { key ->
            loadedTile(key, clearedTile, clearedOwner)
        }

        try {
            clearedCache.clear()

            assertSame(
                shared,
                otherCache.getOrPut(sharedTile) { error("Other owner's shared tile was evicted") }
            )
        } finally {
            clearedCache.clear()
            otherCache.clear()
        }
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
