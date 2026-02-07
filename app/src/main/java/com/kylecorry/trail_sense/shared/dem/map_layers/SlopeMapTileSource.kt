package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.Dither
import com.kylecorry.andromeda.bitmaps.operations.ReplaceColor
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorStrategy
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.shared.withId
import kotlin.math.absoluteValue

class SlopeMapTileSource : TileSource {

    override suspend fun loadTile(context: Context, tile: Tile, params: Bundle): Bitmap? {
        val preferences = params.getBundle(TileSource.PARAM_PREFERENCES)
        val strategyId = preferences?.getString(COLOR)?.toLongOrNull()
        val colorMap = SlopeColorMapFactory().getSlopeColorMap(
            SlopeColorStrategy.entries.withId(strategyId ?: 0) ?: DEFAULT_COLOR
        )
        val highResolution =
            preferences?.getBoolean(
                HIGH_RESOLUTION,
                DEFAULT_HIGH_RESOLUTION
            ) ?: DEFAULT_HIGH_RESOLUTION
        val smooth = preferences?.getBoolean(SMOOTH, DEFAULT_SMOOTH)
            ?: DEFAULT_SMOOTH
        val hideFlatGround =
            preferences?.getBoolean(
                HIDE_FLAT_GROUND,
                DEFAULT_HIDE_FLAT_GROUND
            ) ?: DEFAULT_HIDE_FLAT_GROUND

        val zoomLevel = tile.z.coerceIn(DEM.IMAGE_MIN_ZOOM_LEVEL, DEM.IMAGE_MAX_ZOOM_LEVEL)
        val bounds = tile.getBounds()

        val zoomToResolutionMap = if (highResolution) {
            DEM.HIGH_RESOLUTION_ZOOM_TO_RESOLUTION
        } else {
            DEM.LOW_RESOLUTION_ZOOM_TO_RESOLUTION
        }
        val resolution = zoomToResolutionMap[zoomLevel] ?: return null

        val cellSizeX = getCellSizeX(resolution, bounds)
        val cellSizeY = getCellSizeY(resolution)

        val flatColor = colorMap.getSlopeColor(0f)

        val padding = 2
        return DEM.getElevationImage(
            bounds,
            resolution,
            tile.size,
            config = Bitmap.Config.ARGB_8888,
            padding = padding
        ) { x, y, getElevation ->
            val vector = getSlopeVector(cellSizeX, cellSizeY, x, y, getElevation)
            val slopeDegrees = getSlopeAngle(vector).toDegrees().absoluteValue

            if (hideFlatGround && slopeDegrees <= 10f) {
                return@getElevationImage flatColor
            }

            val actualDegrees = if (smooth) {
                slopeDegrees
            } else {
                when {
                    slopeDegrees <= 10f -> 0f
                    slopeDegrees <= 25f -> 10f
                    else -> 90f
                }
            }

            colorMap.getSlopeColor(actualDegrees)
        }.applyOperationsOrNull(
            Conditional(
                !hideFlatGround,
                Dither(Bitmap.Config.RGB_565)
            ),
            Conditional(
                hideFlatGround,
                ReplaceColor(
                    flatColor,
                    Color.TRANSPARENT,
                    80f,
                    interpolate = false,
                    inPlace = true
                )
            )
        )
    }

    companion object {
        const val SOURCE_ID = "slope"
        const val COLOR = "color"
        const val HIGH_RESOLUTION = "high_resolution"
        const val SMOOTH = "smooth"
        const val HIDE_FLAT_GROUND = "hide_flat_ground"
        val DEFAULT_COLOR = SlopeColorStrategy.GreenToRed
        const val DEFAULT_HIGH_RESOLUTION = false
        const val DEFAULT_SMOOTH = true
        const val DEFAULT_HIDE_FLAT_GROUND = false
    }

}
