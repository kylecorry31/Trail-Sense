package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class TileQueueTest {

    private lateinit var tileQueue: TileQueue
    private lateinit var mockProjection: IMapViewProjection
    private lateinit var mockBounds: CoordinateBounds

    @BeforeEach
    fun setUp() {
        tileQueue = TileQueue()
        mockProjection = mock {
            on { resolutionPixels } doReturn 10f
            on { center } doReturn Coordinate(0.0, 0.0)
            on { toPixels(any<Coordinate>()) } doReturn Vector2(0f, 0f)
        }
        mockBounds = mock {
            on { north } doReturn 1.0
            on { south } doReturn -1.0
            on { east } doReturn 1.0
            on { west } doReturn -1.0
        }
    }

    @Test
    fun enqueueAddsTileToQueue() {
        val tile = Tile(0, 0, 1)
        val imageTile1 = ImageTile("test-key-1", tile) { null }
        val imageTile2 = ImageTile("test-key-2", tile) { null }

        tileQueue.enqueue(imageTile1)
        tileQueue.enqueue(imageTile2)

        assertEquals(2, tileQueue.count())
    }

    @Test
    fun enqueueDoesNotAddDuplicateKeys() {
        val tile = Tile(0, 0, 1)
        val imageTile1 = ImageTile("test-key", tile) { null }
        val imageTile2 = ImageTile("test-key", tile) { null }

        tileQueue.enqueue(imageTile1)
        tileQueue.enqueue(imageTile2)

        assertEquals(1, tileQueue.count())
    }

    @Test
    fun clearRemovesAllTiles() {
        val tile = Tile(0, 0, 1)
        val imageTile = ImageTile("test-key", tile) { null }
        tileQueue.enqueue(imageTile)

        tileQueue.clear()

        assertEquals(0, tileQueue.count())
    }

    @Test
    fun getLoadingCountReturnsCorrectCount() {
        assertEquals(0, tileQueue.getLoadingCount())
    }

    @Test
    fun loadDoesNothingWithoutProjection() = runBlocking {
        val tile = Tile(0, 0, 1)
        var called = false
        val imageTile = ImageTile("test-key", tile) {
            called = true
            null
        }
        tileQueue.enqueue(imageTile)

        tileQueue.load(10, 5)

        assertEquals(1, tileQueue.count())
        assertEquals(false, called)
    }

    @Test
    fun canLoad() = runBlocking {
        val newProjection = mock<IMapViewProjection> {
            on { resolutionPixels } doReturn 20f
            on { center } doReturn Coordinate(1.0, 1.0)
            on { toPixels(any<Coordinate>()) } doReturn Vector2(10f, 10f)
        }

        val mockSize = mock<Size> {
            on { width } doReturn 256
            on { height } doReturn 256
        }
        val mockBitmap = mock<Bitmap>()
        val tile = Tile(0, 0, 1, mockSize)
        val desiredTiles = listOf(tile)

        tileQueue.setMapState(newProjection, desiredTiles)

        var called = false
        var expectedResponse: ImageTile? = null
        val imageTile = ImageTile("test-key", tile) {
            called = true
            mockBitmap
        }

        tileQueue.setChangeListener {
            expectedResponse = it
        }

        tileQueue.enqueue(imageTile)
        assertNull(expectedResponse)
        assertEquals(TileState.Idle, imageTile.state)

        tileQueue.load(10, 5)

        var image: Bitmap? = null
        imageTile.withImage { image = it }

        assertEquals(0, tileQueue.count())
        assertEquals(true, called)
        assertEquals(imageTile, expectedResponse)
        assertEquals(mockBitmap, image)
        assertEquals(TileState.Loaded, imageTile.state)
    }
}