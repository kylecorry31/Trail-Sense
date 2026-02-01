package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.alpha
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.MapPixels
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.AlphaColorMap
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileImageUtils
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Instant

class NightTileSource : TileSource {

    var smooth = false
    private val colorMap = AlphaColorMap(maxAlpha = 200)
    private val astronomy = AstronomyService()

    override suspend fun loadTile(tile: Tile, params: Bundle): Bitmap? {
        val time = Instant.ofEpochMilli(params.getLong(TileSource.PARAM_TIME))
            .toZonedDateTime()
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

        val resolution = TileImageUtils.getRequiredResolution(tile, 20)
        return TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            getValues = TileImageUtils.parallelGridEvaluation { lat, lon ->
                astronomy.getSunAltitude(Coordinate(lat, lon), time)
            }
        ) { x, y, getValue ->
            val value = getValue(x, y)
            if (smooth) {
                val pct =
                    SolMath.norm(value, 0f, AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL, true)
                colorMap.getColor(1 - pct)
            } else {
                val gray = (255 * SolMath.norm(
                    value,
                    AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL - 5f,
                    5f,
                    true
                )).toInt()
                return@getSampledImage Color.argb(gray, 0, 0, 0)
            }
        }.applyOperationsOrNull(
            Conditional(
                !smooth,
                MapPixels(true) {
                    val value = SolMath.lerp(
                        it.alpha / 255f,
                        AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL - 5f,
                        5f
                    )
                    val pct = when {
                        value <= AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL -> 1f
                        value <= AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL -> 0.75f
                        value <= AstronomyService.SUN_MIN_ALTITUDE_CIVIL -> 0.5f
                        value <= 0 -> 0.25f
                        else -> 0f
                    }
                    colorMap.getColor(1 - pct)
                }
            )
        )
    }
}
