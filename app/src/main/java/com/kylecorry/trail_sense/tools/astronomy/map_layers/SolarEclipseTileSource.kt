package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.graphics.Bitmap
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.AlphaColorMap
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileImageUtils
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.ZonedDateTime

class SolarEclipseTileSource : TileSource {

    var smooth = false
    private val colorMap = AlphaColorMap()
    private val astronomy = AstronomyService()

    override suspend fun loadTile(tile: Tile): Bitmap? {
        val time = ZonedDateTime.now()
        val bounds = tile.getBounds()
        val isEclipseVisible = arrayOf(
            bounds.northWest,
            bounds.southWest,
            bounds.center,
            bounds.northEast,
            bounds.southEast
        ).any {
            astronomy.getSolarEclipseObscuration(it, time) != null
        }

        if (!isEclipseVisible) {
            return null
        }

        val resolution = TileImageUtils.getRequiredResolution(tile, if (smooth) 5 else 10)
        return TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            smoothPixelEdges = !smooth,
            getValues = TileImageUtils.parallelGridEvaluation { lat, lon ->
                astronomy.getSolarEclipseObscuration(
                    Coordinate(lat, lon),
                    time
                ) ?: 0f
            }
        ) { x, y, getValue ->
            val value = getValue(x, y)
            val newValue = if (smooth) {
                value
            } else {
                when {
                    SolMath.isApproximatelyEqual(value, 1f) -> 1f
                    value >= 0.9f -> 0.95f
                    value >= 0.8f -> 0.9f
                    value >= 0.7f -> 0.8f
                    value >= 0.6f -> 0.7f
                    value >= 0.5f -> 0.6f
                    value >= 0.4f -> 0.5f
                    value >= 0.3f -> 0.4f
                    value >= 0.2f -> 0.3f
                    value >= 0.1f -> 0.2f
                    value > 0f -> 0.1f
                    else -> 0f
                }
            }
            colorMap.getColor(1 - newValue)
        }
    }
}
