package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.bitmaps.operations.Dither
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeAspect
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

class HillshadeMapTileSource : TileSource {
    var drawAccurateShadows: Boolean = false
    var highResolution: Boolean = false
    var multiDirectionShading: Boolean = false
    private val astronomy = AstronomyService()

    override suspend fun loadTile(tile: Tile, params: Bundle): Bitmap? {
        val time = Instant.ofEpochMilli(params.getLong(TileSource.PARAM_TIME))
        val zonedDateTime = time.toZonedDateTime()
        val zoomLevel = tile.z.coerceIn(DEM.IMAGE_MIN_ZOOM_LEVEL, DEM.IMAGE_MAX_ZOOM_LEVEL)
        val bounds = tile.getBounds()
        val zFactor = 3f
        val samples = if (multiDirectionShading) 5 else 1
        val sampleSpacing = 45f
        val (azimuth, altitude) = getShadowConfig(bounds.center, zonedDateTime)
        val zoomToResolutionMap = if (highResolution) {
            DEM.HIGH_RESOLUTION_ZOOM_TO_RESOLUTION
        } else {
            DEM.LOW_RESOLUTION_ZOOM_TO_RESOLUTION
        }
        val resolution = zoomToResolutionMap[zoomLevel] ?: return null

        val cellSizeX = getCellSizeX(resolution, bounds)
        val cellSizeY = getCellSizeY(resolution)
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
            val vector = getSlopeVector(cellSizeX, cellSizeY, x, y, getElevation)
            val slopeRad = getSlopeAngle(vector, zFactor)
            val aspectRad = getSlopeAspect(vector)

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

    private fun getShadowConfig(location: Coordinate, time: ZonedDateTime): Pair<Float, Float> {
        if (!drawAccurateShadows) {
            return 315f to 45f
        }

        if (astronomy.getSunAltitude(location, time) > AstronomyService.SUN_MIN_ALTITUDE_CIVIL) {
            return astronomy.getSunAzimuth(location, time).value to astronomy.getSunAltitude(
                location,
                time
            )
        }

        if (astronomy.isMoonUp(
                location,
                time
            ) && Astronomy.getMoonPhase(time).illumination > 0.25f
        ) {
            return astronomy.getMoonAzimuth(location, time).value to astronomy.getMoonAltitude(
                location,
                time
            )
        }

        return 315f to 45f
    }
}