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

class NightTileSource : TileSource {

    var smooth = false
    private val colorMap = AlphaColorMap()
    private val astronomy = AstronomyService()

    override suspend fun loadTile(tile: Tile): Bitmap? {
        val time = ZonedDateTime.now()
        val bounds = tile.getBounds()
        val isNight = arrayOf(
            bounds.northWest,
            bounds.southWest,
            bounds.center,
            bounds.northEast,
            bounds.southEast
        ).any {
            astronomy.getSunAltitude(it, time) < 0
        }

        if (!isNight) {
            return null
        }

        val resolution = TileImageUtils.getRequiredResolution(tile, if (smooth) 10 else 20)
        return TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            smoothPixelEdges = !smooth,
            getValues = TileImageUtils.parallelGridEvaluation { lat, lon ->
                astronomy.getSunAltitude(Coordinate(lat, lon), time)
            }
        ) { x, y, getValue ->
            val value = getValue(x, y)
            val newValue = if (smooth) {
                SolMath.norm(value, 0f, AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL, true)
            } else {
                when {
                    value <= AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL -> 1f
                    value <= AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL -> 0.75f
                    value <= AstronomyService.SUN_MIN_ALTITUDE_CIVIL -> 0.5f
                    value <= 0 -> 0.25f
                    else -> 0f
                }
            }
            colorMap.getColor(1 - newValue)
        }
    }
}
