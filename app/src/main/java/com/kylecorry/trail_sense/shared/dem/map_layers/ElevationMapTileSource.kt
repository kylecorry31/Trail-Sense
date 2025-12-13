package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.units.PercentBounds
import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.andromeda_temp.CorrectPerspective2
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMap
import com.kylecorry.trail_sense.shared.dem.colors.USGSElevationColorMap
import com.kylecorry.trail_sense.shared.map_layers.tiles.IGeographicImageRegionLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.ITileSourceSelector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ElevationMapTileSource : ITileSourceSelector {

    var useDynamicElevationScale = false
    var colorScale: ElevationColorMap = USGSElevationColorMap()
    private var loaderLock = Mutex()
    private var lastLoader: ElevationRegionLoader? = null

    override suspend fun getRegionLoaders(bounds: List<CoordinateBounds>): List<List<IGeographicImageRegionLoader>> {
        val fullBounds = CoordinateBounds.from(bounds.flatMap {
            listOf(
                it.northWest,
                it.southEast
            )
        })
        return loaderLock.withLock {
            lastLoader?.close()
            lastLoader = ElevationRegionLoader(fullBounds, useDynamicElevationScale, colorScale)
            bounds.map { listOfNotNull(lastLoader) }
        }
    }

    override suspend fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader> =
        emptyList()

    private class ElevationRegionLoader(
        private val fullBounds: CoordinateBounds,
        private val useDynamicElevationScale: Boolean = false,
        private var colorScale: ElevationColorMap = USGSElevationColorMap()
    ) : IGeographicImageRegionLoader {
        private val minScaleElevation = 0f
        private val minZoomLevel = 10
        private val maxZoomLevel = 19
        private val baseResolution = 1 / 240.0
        private val validResolutions = mapOf(
            10 to baseResolution * 8,
            11 to baseResolution * 4,
            12 to baseResolution * 2,
            13 to baseResolution,
            14 to baseResolution / 2,
            15 to baseResolution / 4,
            16 to baseResolution / 4,
            17 to baseResolution / 4,
            18 to baseResolution / 4,
            19 to baseResolution / 4
        )
        private var elevations: Bitmap? = null
        private var isStopped = false
        private val lock = Mutex()

        suspend fun close() = lock.withLock {
            isStopped = true
            elevations?.recycle()
            elevations = null
        }

        override suspend fun load(tile: Tile): Bitmap? {
            val fullImage = lock.withLock {
                if (elevations == null && !isStopped) {
                    elevations = loadFullImage(fullBounds, tile.z)
                }
                elevations
            } ?: return null

            val bounds = tile.getBounds()

            // TODO: There's a gap sometimes
            val leftPercent = (bounds.west - fullBounds.west) / fullBounds.widthDegrees()
            val rightPercent = (bounds.east - fullBounds.west) / fullBounds.widthDegrees()
            val topPercent = (bounds.north - fullBounds.south) / fullBounds.heightDegrees()
            val bottomPercent = (bounds.south - fullBounds.south) / fullBounds.heightDegrees()
            val percentBottomLeft =
                PercentCoordinate(leftPercent.toFloat(), bottomPercent.toFloat())
            val percentBottomRight =
                PercentCoordinate(rightPercent.toFloat(), bottomPercent.toFloat())
            val percentTopLeft = PercentCoordinate(leftPercent.toFloat(), topPercent.toFloat())
            val percentTopRight = PercentCoordinate(rightPercent.toFloat(), topPercent.toFloat())

            return lock.withLock {
                if (fullImage.isRecycled) {
                    return null
                }
                fullImage.applyOperationsOrNull(
                    CorrectPerspective2(
                        // Bounds are inverted on the Y axis from android's pixel coordinate system
                        PercentBounds(
                            percentBottomLeft,
                            percentBottomRight,
                            percentTopLeft,
                            percentTopRight
                        ),
                        maxSize = tile.size,
                        outputSize = tile.size
                    ),
                    recycleOriginal = false,
                )
            }
        }

        private suspend fun loadFullImage(
            bounds: CoordinateBounds,
            zoomLevel: Int
        ): Bitmap? {
            val zoomLevel = zoomLevel.coerceIn(minZoomLevel, maxZoomLevel)

            return DEM.elevationImage(
                bounds,
                validResolutions[zoomLevel]!!
            ) { elevation, _, maxElevation ->
                if (useDynamicElevationScale) {
                    var max = (maxElevation * 1.25f).roundNearest(1000f)
                    if (max < maxElevation) {
                        max += 1000f
                    }
                    colorScale.getColor(
                        SolMath.norm(
                            elevation,
                            minScaleElevation,
                            max,
                            true
                        )
                    )
                } else {
                    colorScale.getElevationColor(elevation)
                }
            }
        }

    }
}