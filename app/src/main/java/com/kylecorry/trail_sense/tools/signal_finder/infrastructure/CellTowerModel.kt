package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import com.kylecorry.trail_sense.shared.data.TiledImageReader

object CellTowerModel {

    // Image data source
    private val resolution = 0.015
    private val size = Size(12000, 6000)

    val accuracy = Distance.nauticalMiles(resolution.toFloat() * 60 / 2f).meters()

    private val source = GeographicImageSource(
        EncodedDataImageReader(
            TiledImageReader(
                listOf(
                    Rect(0, 0, size.width, size.height) to SingleImageReader(
                        size,
                        AssetInputStreamable("cell_towers/cell_towers_0.webp")
                    ),
                    Rect(size.width, 0, size.width * 2, size.height) to SingleImageReader(
                        size,
                        AssetInputStreamable("cell_towers/cell_towers_1.webp")
                    ),
                    Rect(0, size.height, size.width, size.height * 2) to SingleImageReader(
                        size,
                        AssetInputStreamable("cell_towers/cell_towers_2.webp")
                    ),
                    Rect(
                        size.width,
                        size.height,
                        size.width * 2,
                        size.height * 2
                    ) to SingleImageReader(
                        size,
                        AssetInputStreamable("cell_towers/cell_towers_3.webp")
                    )
                )
            ),
            decoder = EncodedDataImageReader.scaledDecoder(1.0, 0.0, false),
            maxChannels = 1
        ),
        interpolationOrder = 0
    )

    // TODO: Load the whole region of the image and get the towers from it
    suspend fun getTowers(
        bounds: CoordinateBounds,
        count: Int? = null
    ): List<Coordinate> = onIO {
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
            .sortedBy { it.distanceTo(bounds.center) }
            .take(count ?: Int.MAX_VALUE)
    }

    private suspend fun getTowers(
        locations: List<Coordinate>
    ): List<Coordinate> = onIO {
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
                location
            }

        results
    }

}