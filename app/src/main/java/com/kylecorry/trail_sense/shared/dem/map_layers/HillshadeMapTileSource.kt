package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import com.kylecorry.andromeda.bitmaps.operations.Convert
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.Flip
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

    override suspend fun load(tiles: List<Tile>, onLoaded: suspend (Tile, Bitmap?) -> Unit) {
        Parallel.forEach(tiles) {
            val bitmap = loadTile(it)
            onLoaded(it, bitmap)
        }
    }

    private suspend fun loadTile(tile: Tile): Bitmap? {
        val zoomLevel = tile.z.coerceIn(minZoomLevel, maxZoomLevel)
        val bounds = tile.getBounds()
        val zFactor = 3f
        val samples = 1
        val sampleSpacing = 3f
        val (azimuth, altitude) = getShadowConfig(bounds.center)
        val resolution = validResolutions[zoomLevel]!!

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

        return DEM.getElevationImage(bounds, resolution) { x, y, getElevation ->
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
            Convert(Bitmap.Config.ARGB_8888),
            Resize(Size(10, 10), true),
            Flip(horizontal = false)
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