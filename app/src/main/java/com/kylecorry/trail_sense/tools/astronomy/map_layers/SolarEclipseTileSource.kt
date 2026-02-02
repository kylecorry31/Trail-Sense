package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.alpha
import com.kylecorry.andromeda.bitmaps.LookupTable
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.AlphaColorMap
import com.kylecorry.trail_sense.shared.andromeda_temp.Lut
import com.kylecorry.trail_sense.shared.map_layers.tiles.InterpolatedGridValueProvider
import com.kylecorry.trail_sense.shared.map_layers.tiles.ParallelCoordinateGridValueProvider
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
    private val lookupTable by lazy { constructLookupTable() }

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

        val resolution = TileImageUtils.getRequiredResolution(tile, 128)
        val bitmap = TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            valueProvider = InterpolatedGridValueProvider(
                4,
                ParallelCoordinateGridValueProvider { lat, lon ->
                    getEclipseObscuration(Coordinate(lat, lon), time) ?: 0f
                })
        ) { x, y, getValue ->
            val value = getValue(x, y)
            if (smooth) {
                colorMap.getColor(value)
            } else {
                Color.argb((255 * value.coerceIn(0f, 1f)).toInt(), 0, 0, 0)
            }
        }.applyOperationsOrNull(
            Conditional(
                !smooth,
                Lut(lookupTable)
            )
        )
        return bitmap
    }

    private fun getEclipseObscuration(location: Coordinate, time: ZonedDateTime): Float? {
        return if (showPath) {
            astronomy.getPeakSolarEclipseObscuration(location, time)
        } else {
            astronomy.getSolarEclipseObscuration(location, time)
        }?.coerceIn(0f, 1f)
    }

    private fun constructLookupTable(): LookupTable {
        val table = LookupTable()
        for (i in table.alpha.indices) {
            val value = i / 255f
            val pct = when {
                value >= 0.98f -> 1f
                value >= 0.9f -> 0.95f
                value >= 0.8f -> 0.9f
                value >= 0.7f -> 0.8f
                value >= 0.6f -> 0.7f
                value >= 0.5f -> 0.6f
                value >= 0.4f -> 0.5f
                value >= 0.3f -> 0.4f
                value >= 0.2f -> 0.3f
                value >= 0.1f -> 0.2f
                value > 0.05f -> 0.1f
                else -> 0f
            }
            table.alpha[i] = colorMap.getColor(pct).alpha.toByte()
        }
        return table
    }
}
