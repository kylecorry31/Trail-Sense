package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.bitmaps.operations.getPixels
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.ImageReader
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil
import kotlin.math.floor

object CellTowerModel {

    private const val CELLS_PER_DEGREE = 192
    private const val RESOLUTION = 1.0 / CELLS_PER_DEGREE
    private val size = Size(1024, 384)
    private val reader = SingleImageReader(size, AssetInputStreamable("cell_towers/cell_towers.webp"))

    // Accounts for errors in the dataset
    private const val ACCURACY_SCALE = 2f

    private val indexLock = Mutex()
    private var index: CellTowerIndex? = null

    fun getAccuracy(towerLocation: Coordinate): Distance {
        return Distance.meters(
            ACCURACY_SCALE * towerLocation.distanceTo(
                Coordinate(
                    towerLocation.latitude,
                    towerLocation.longitude + RESOLUTION / 2
                )
            )
        )
    }

    private suspend fun getIndex(): CellTowerIndex = indexLock.withLock {
        index ?: CellTowerIndex.open(ImageByteSource(reader), CELLS_PER_DEGREE).also { index = it }
    }

    // The index is stored as the pixels of an image (a byte per pixel, read left to right, top to bottom), so
    // only the rows with the requested bytes are decoded
    private class ImageByteSource(private val reader: ImageReader) : CellTowerIndex.ByteSource {
        override suspend fun read(offset: Int, size: Int): ByteArray {
            val imageSize = reader.getSize()
            val width = imageSize.width
            val firstRow = offset / width
            val lastRow = minOf((offset + size - 1) / width, imageSize.height - 1)
            if (firstRow > lastRow) {
                return ByteArray(0)
            }

            var bytes: ByteArray? = null
            reader.getRegion(Rect(0, firstRow, width, lastRow + 1))?.use {
                val pixels = getPixels()
                bytes = ByteArray(pixels.size) { i -> pixels[i].red.toByte() }
            }
            val rows = bytes ?: throw IllegalStateException("Cell tower model not found")

            val start = offset - firstRow * width
            return rows.copyOfRange(start, minOf(start + size, rows.size))
        }
    }

    suspend fun getTowers(
        bounds: CoordinateBounds,
        count: Int? = null
    ): List<ApproximateCoordinate> = onIO {
        val index = getIndex()

        val minRow = floor((bounds.south + 90) * CELLS_PER_DEGREE).toInt().coerceAtLeast(0)
        val maxRow = ceil((bounds.north + 90) * CELLS_PER_DEGREE).toInt()
            .coerceAtMost(180 * CELLS_PER_DEGREE)

        // The bounds wrap around the antimeridian when the west is east of the east
        val columnRanges = if (bounds.west <= bounds.east) {
            listOf(bounds.west to bounds.east)
        } else {
            listOf(bounds.west to 180.0, -180.0 to bounds.east)
        }

        columnRanges.flatMap { (west, east) ->
            val minColumn = floor((west + 180) * CELLS_PER_DEGREE).toInt().coerceAtLeast(0)
            val maxColumn = ceil((east + 180) * CELLS_PER_DEGREE).toInt()
                .coerceAtMost(360 * CELLS_PER_DEGREE)
            index.getCells(minRow, maxRow, minColumn, maxColumn)
        }.mapNotNull { cell ->
            val location = Coordinate(
                (cell shr 32).toInt() * RESOLUTION - 90,
                cell.toInt() * RESOLUTION - 180
            )
            if (bounds.contains(location)) {
                ApproximateCoordinate(
                    location.latitude,
                    location.longitude,
                    getAccuracy(location)
                )
            } else {
                null
            }
        }
            .sortedBy { it.coordinate.distanceTo(bounds.center) }
            .take(count ?: Int.MAX_VALUE)
    }

}
