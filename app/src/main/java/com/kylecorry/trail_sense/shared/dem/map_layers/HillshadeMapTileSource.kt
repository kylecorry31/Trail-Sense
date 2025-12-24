package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.FullRegionMapTileLoader
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.FullRegionMapTileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService

class HillshadeMapTileSource : FullRegionMapTileSource() {
    var drawAccurateShadows: Boolean = false
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
    private val astronomy = AstronomyService()

    override fun getLoader(fullBounds: CoordinateBounds): FullRegionMapTileLoader {
        return object : FullRegionMapTileLoader(fullBounds, Size(10, 10)) {
            override suspend fun loadFullImage(
                bounds: CoordinateBounds,
                zoomLevel: Int
            ): Bitmap? {
                val (azimuth, altitude) = getShadowConfig(bounds.center)
                val zoomLevel = zoomLevel.coerceIn(minZoomLevel, maxZoomLevel)
                return DEM.hillshadeImage(
                    bounds,
                    validResolutions[zoomLevel]!!,
                    3f,
                    azimuth,
                    altitude
                )
            }

            private fun getShadowConfig(location: Coordinate): Pair<Float, Float> {
                if (!drawAccurateShadows) {
                    return 315f to 45f
                }

                if (astronomy.isSunUp(location)) {
                    return astronomy.getSunAzimuth(location).value to astronomy.getSunAltitude(
                        location
                    )
                }

                if (astronomy.isMoonUp(location) && astronomy.getCurrentMoonPhase().illumination > 0.25f) {
                    return astronomy.getMoonAzimuth(location).value to astronomy.getMoonAltitude(
                        location
                    )
                }

                return 315f to 45f
            }

        }
    }
}