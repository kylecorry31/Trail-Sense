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
import java.time.ZonedDateTime

class SolarEclipseTileSource : TileSource {

    var smooth = false
    var showPath = false
    private val colorMap = AlphaColorMap(maxAlpha = 200)
    private val astronomy = AstronomyService()

    override suspend fun loadTile(tile: Tile, params: Bundle): Bitmap? {
        val time = Instant.ofEpochMilli(params.getLong(TileSource.PARAM_TIME))
            .toZonedDateTime()
        val bounds = tile.getBounds()
        val isEclipseVisible = arrayOf(
            bounds.northWest,
            bounds.southWest,
            bounds.center,
            bounds.northEast,
            bounds.southEast
        ).any {
            getEclipseObscuration(it, time) != null
        }

        if (!isEclipseVisible) {
            return null
        }

        val resolution = TileImageUtils.getRequiredResolution(tile, 5)
        val bitmap = TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            getValues = TileImageUtils.parallelGridEvaluation { lat, lon ->
                getEclipseObscuration(Coordinate(lat, lon), time) ?: 0f
            }
        ) { x, y, getValue ->
            val value = getValue(x, y)
            if (smooth) {
                colorMap.getColor(1 - value)
            } else {
                Color.argb((255 * value).toInt(), 0, 0, 0)
            }
        }.applyOperationsOrNull(
            Conditional(
                !smooth,
                MapPixels(true) {
                    val value = it.alpha / 255f
                    val pct = when {
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
                    colorMap.getColor(1 - pct)
                }
            )
        )
        return bitmap
    }

    private fun getEclipseObscuration(location: Coordinate, time: ZonedDateTime): Float? {
        return if (showPath) {
            astronomy.getSolarEclipse(location, time.toLocalDate())?.obscuration
        } else {
            astronomy.getSolarEclipseObscuration(location, time)
        }?.coerceIn(0f, 1f)
    }
}
