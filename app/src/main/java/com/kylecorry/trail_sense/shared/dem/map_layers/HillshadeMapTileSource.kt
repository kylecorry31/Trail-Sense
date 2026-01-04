package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.Dither
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class HillshadeMapTileSource : TileSource {
    var drawAccurateShadows: Boolean = false
    var highResolution: Boolean = false
    private val astronomy = AstronomyService()

    override suspend fun load(tiles: List<Tile>, onLoaded: suspend (Tile, Bitmap?) -> Unit) {
        Parallel.forEach(tiles, 16) {
            val bitmap = loadTile(it)
            onLoaded(it, bitmap)
        }
    }

    private suspend fun loadTile(tile: Tile): Bitmap? {
        val zoomLevel = tile.z.coerceIn(DEM.IMAGE_MIN_ZOOM_LEVEL, DEM.IMAGE_MAX_ZOOM_LEVEL)
        val bounds = tile.getBounds()
        val zFactor = 3f
        val samples = 1
        val sampleSpacing = 3f
        val (azimuth, altitude) = getShadowConfig(bounds.center)
        val zoomToResolutionMap = if (highResolution) {
            DEM.HIGH_RESOLUTION_ZOOM_TO_RESOLUTION
        } else {
            DEM.LOW_RESOLUTION_ZOOM_TO_RESOLUTION
        }
        val resolution = zoomToResolutionMap[zoomLevel] ?: return null

        val cellSizeX = (resolution * 111319.5 * cosDegrees(bounds.center.latitude))
        val cellSizeY = (resolution * 111319.5)
        // https://pro.arcgis.com/en/pro-app/latest/tool-reference/3d-analyst/how-hillshade-works.htm
        val zenithRad = (90 - altitude).toRadians()
        val azimuths = mutableListOf<Float>()
        var start = azimuth - (samples / 2f) * sampleSpacing
        for (i in 0 until samples) {
            azimuths.add(Trigonometry.remapUnitAngle(start, 90f, true).toRadians())
            start += sampleSpacing
        }
        val cosZenith = cos(zenithRad)
        val sinZenith = sin(zenithRad)

        val padding = 2
        return DEM.getElevationImage(
            bounds,
            resolution,
            tile.size,
            config = Bitmap.Config.ARGB_8888,
            padding = padding
        ) { x, y, getElevation ->
            val a = getElevation(x - 1, y - 1)
            val b = getElevation(x, y - 1)
            val c = getElevation(x + 1, y - 1)
            val d = getElevation(x - 1, y)
            val f = getElevation(x + 1, y)
            val g = getElevation(x - 1, y + 1)
            val h = getElevation(x, y + 1)
            val i = getElevation(x + 1, y + 1)
            val dx = (((c + 2 * f + i) - (a + 2 * d + g)) / (8 * cellSizeX)).toFloat()
            val dy = (((g + 2 * h + i) - (a + 2 * b + c)) / (8 * cellSizeY)).toFloat()
            val slopeRad = atan(zFactor * hypot(dx, dy))

            var aspectRad = 0f
            if (!SolMath.isZero(dx)) {
                aspectRad = wrap(atan2(dy, -dx), 0f, 2 * PI.toFloat())
            } else {
                if (dy > 0) {
                    aspectRad = PI.toFloat() / 2
                } else if (dy < 0) {
                    aspectRad = 3 * PI.toFloat() / 2
                }
            }

            var hillshade = 0.0
            for (azimuthRad in azimuths) {
                hillshade += 255 * (cosZenith * cos(slopeRad) +
                        sinZenith * sin(slopeRad) * cos(azimuthRad - aspectRad)) / samples
            }

            val gray = hillshade.toInt().coerceIn(0, 255)
            Color.rgb(gray, gray, gray)
        }.applyOperationsOrNull(
            Dither(Bitmap.Config.RGB_565)
        )
    }

    private fun getShadowConfig(location: Coordinate): Pair<Float, Float> {
        if (!drawAccurateShadows) {
            return 315f to 45f
        }

        if (astronomy.getSunAltitude(location) > AstronomyService.SUN_MIN_ALTITUDE_CIVIL) {
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