package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import com.kylecorry.trail_sense.shared.data.TiledImageReader

object CellTowerModel {

    // Image data source
    private val resolution = 0.01
    private val size = Size(9000, 6000)
    private val rows = 3
    private val columns = 4

    // Accounts for errors in the dataset
    private val accuracyScale = 2f

    fun getAccuracy(towerLocation: Coordinate): Distance {
        return Distance.meters(
            accuracyScale * towerLocation.distanceTo(
                Coordinate(
                    towerLocation.latitude,
                    towerLocation.longitude + resolution / 2
                )
            )
        )
    }

    private fun getTileReader(
        rows: Int,
        columns: Int,
        tileSize: Size,
        files: List<String>
    ): TiledImageReader {
        val tileWidth = tileSize.width
        val tileHeight = tileSize.height
        val readers = mutableListOf<Pair<Rect, SingleImageReader>>()
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                val index = r * columns + c
                if (index >= files.size) {
                    break
                }
                val file = files[index]
                val rect = Rect(
                    c * tileWidth,
                    r * tileHeight,
                    (c + 1) * tileWidth,
                    (r + 1) * tileHeight
                )
                readers.add(
                    rect to SingleImageReader(
                        Size(tileWidth, tileHeight),
                        AssetInputStreamable(file)
                    )
                )
            }
        }
        return TiledImageReader(readers)
    }

    private fun getFiles(count: Int): List<String> {
        val files = mutableListOf<String>()
        for (i in 0 until count) {
            files.add("cell_towers/cell_towers_$i.webp")
        }
        return files
    }

    private val source = GeographicImageSource(
        EncodedDataImageReader(
            getTileReader(rows, columns, size, getFiles(rows * columns)),
            decoder = EncodedDataImageReader.scaledDecoder(1.0, 0.0, false),
            maxChannels = 1
        ),
        interpolationOrder = 0
    )

    // TODO: Load the whole region of the image and get the towers from it
    suspend fun getTowers(
        bounds: CoordinateBounds,
        count: Int? = null
    ): List<ApproximateCoordinate> = onIO {
        val locations = mutableListOf<Coordinate>()

        val latitudes = Interpolation.getMultiplesBetween(
            bounds.south - resolution,
            bounds.north + resolution,
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween(
            bounds.west - resolution,
            bounds.east + resolution,
            resolution
        )

        latitudes.forEach { lat ->
            longitudes.forEach { lon ->
                locations.add(Coordinate(lat, lon))
            }
        }

        // Remove locations that are outside the geofence
        locations.removeIf { !bounds.contains(it) }

        getTowers(locations)
            .sortedBy { it.coordinate.distanceTo(bounds.center) }
            .take(count ?: Int.MAX_VALUE)
    }

    private suspend fun getTowers(
        locations: List<Coordinate>
    ): List<ApproximateCoordinate> = onIO {
        val pixelsToLoad = locations.associate {
            val rounded = it.copy(
                latitude = it.latitude.roundNearest(resolution),
                longitude = it.longitude.roundNearest(resolution)
            )
            val pixel = source.getPixel(rounded)
            pixel to rounded
        }

        val results = source.read(pixelsToLoad.keys.toList())
            .mapNotNull {
                val data = it.second
                if (data.isEmpty()) {
                    return@mapNotNull null
                }
                val location = pixelsToLoad[it.first] ?: return@mapNotNull null
                val hasTower = data[0].toInt() > 0
                if (!hasTower) {
                    return@mapNotNull null
                }
                ApproximateCoordinate(location.latitude, location.longitude, getAccuracy(location))
            }

        results
    }

}