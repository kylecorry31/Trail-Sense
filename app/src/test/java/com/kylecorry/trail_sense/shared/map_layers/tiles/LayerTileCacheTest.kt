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
        val first = cache.getOrPut(listOf(firstTile, secondTile)) { tile, key ->
            loadedTile(key, tile, owner)
        }.first()

        assertSame(first, cache.peek(firstTile))
        assertSame(
            first,
            cache.getOrPut(listOf(firstTile)) { tile, key -> loadedTile(key, tile, owner) }.single()
        )
        cache.clear()
    }

    @Test
    fun keepsSharedTileWhenLoadCompletesWithoutImage() = runBlocking {
        val owner = UUID.randomUUID().toString()
        val cache = LayerTileCache(owner, 1)
        val firstTile = Tile(0, 0, 1)
        val secondTile = Tile(1, 0, 1)
        val first = cache.getOrPut(listOf(firstTile, secondTile)) { tile, key ->
            if (tile == firstTile) {
                ImageTile(
                    key = key,
                    tile = tile,
                    state = TileState.Loading,
                    owner = owner,
                    loadFunction = { null }
                )
            } else {
                loadedTile(key, tile, owner)
            }
        }.first()

        first.load()
        cache.onLoadComplete(first)

        try {
            assertEquals(TileState.Empty, first.state)
            assertSame(
                first,
                cache.getOrPut(listOf(firstTile)) { _, _ -> error("Empty tile was loaded again") }.single()
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
        val otherTiles = listOf(sharedTile) + (1..256).map { x -> Tile(x, 0, 9) }
        val shared = otherCache.getOrPut(otherTiles) { tile, key ->
            loadedTile(key, tile, otherOwner)
        }.first()
        val clearedTile = Tile(0, 1, 9)
        clearedCache.getOrPut(listOf(clearedTile)) { tile, key ->
            loadedTile(key, tile, clearedOwner)
        }

        try {
            clearedCache.clear()

            assertSame(
                shared,
                otherCache.getOrPut(listOf(sharedTile)) { _, _ ->
                    error("Other owner's shared tile was evicted")
                }.single()
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
        val (first, second) = cache.getOrPut(listOf(firstTile, secondTile)) { tile, key ->
            loadedTile(key, tile, owner)
        }

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
