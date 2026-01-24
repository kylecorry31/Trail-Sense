package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.trail_sense.shared.andromeda_temp.Dither
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeAspect
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlin.math.absoluteValue

class AspectMapTileSource : TileSource {
    var highResolution: Boolean = false

    private val colorMap = RgbInterpolationColorMap(
        arrayOf(
            AppColor.Green.color,
            Colors.interpolate(AppColor.Green.color, AppColor.Blue.color, 0.5f),
            AppColor.Blue.color,
            AppColor.Purple.color,
            AppColor.Red.color,
            Colors.interpolate(AppColor.Red.color, AppColor.Orange.color, 0.5f),
            AppColor.Orange.color,
            AppColor.Yellow.color,
            AppColor.Green.color
        )
    )

    override suspend fun loadTile(tile: Tile): Bitmap? {
        if (tile.z !in DEM.IMAGE_MIN_ZOOM_LEVEL..DEM.IMAGE_MAX_ZOOM_LEVEL) {
            return null
        }
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

            if (slopeDegrees < 1f) {
                return@getElevationImage AppColor.Gray.color
            }

            val aspect = Trigonometry.remapAngle(
                getSlopeAspect(vector).toDegrees(),
                180f,
                false,
                90f,
                false
            )
            colorMap.getColor(aspect / 360f)
        }.applyOperationsOrNull(
            Dither(Bitmap.Config.RGB_565)
        )
    }

}
