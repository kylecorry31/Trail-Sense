package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.ReplaceColor
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.trail_sense.shared.andromeda_temp.Dither
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.GreenToRedSlopeColorMap
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorMap
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlin.math.absoluteValue

class SlopeMapTileSource : TileSource {
    var highResolution: Boolean = false
    var colorMap: SlopeColorMap = GreenToRedSlopeColorMap()
    var smooth = true
    var hideFlatGround = false

    override suspend fun loadTile(tile: Tile, params: Bundle): Bitmap? {
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

}
