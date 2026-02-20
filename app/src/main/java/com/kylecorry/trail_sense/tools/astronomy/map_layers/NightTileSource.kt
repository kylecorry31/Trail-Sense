package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.alpha
import com.kylecorry.andromeda.bitmaps.LookupTable
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.Lut
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.ui.colormaps.AlphaColorMap
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.tiles.InterpolatedGridValueProvider
import com.kylecorry.trail_sense.shared.map_layers.tiles.ParallelCoordinateGridValueProvider
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileImageUtils
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Instant

class NightTileSource : TileSource {

    private val colorMap = AlphaColorMap(maxAlpha = 200)
    private val astronomy = AstronomyService()

    private val lookupTable by lazy { constructLookupTable() }

    override suspend fun loadTile(context: Context, tile: Tile, params: Bundle): Bitmap? {
        val preferences = params.getPreferences()
        val smooth = preferences.getBoolean(SMOOTH, DEFAULT_SMOOTH)

        val time = Instant.ofEpochMilli(params.getLong(MapLayerParams.PARAM_TIME))
            .toZonedDateTime()
        val bounds = tile.getBounds()
        val isNight = arrayOf(
            bounds.northWest,
            bounds.southWest,
            bounds.center,
            bounds.northEast,
            bounds.southEast
        ).any {
            astronomy.getSunAltitude(it, time) < 1
        }

        if (!isNight) {
            return null
        }

        val resolution = TileImageUtils.getRequiredResolution(tile, 128)
        return TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            valueProvider = InterpolatedGridValueProvider(
                10,
                ParallelCoordinateGridValueProvider { lat, lon ->
                    astronomy.getSunAltitude(Coordinate(lat, lon), time)
                })
        ) { x, y, getValue ->
            val value = getValue(x, y)
            if (smooth) {
                val pct =
                    Interpolation.norm(value, 0f, AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL, true)
                colorMap.getColor(pct)
            } else {
                val gray = (255 * Interpolation.norm(
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
                Lut(lookupTable)
            )
        )
    }

    private fun constructLookupTable(): LookupTable {
        val table = LookupTable()
        for (i in table.alpha.indices) {
            val value = Interpolation.lerp(
                i / 255f,
                AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL - 5f,
                5f
            )
            val pct = when {
                value <= AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL -> 1f
                value <= AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL -> 0.75f
                value <= AstronomyService.SUN_MIN_ALTITUDE_CIVIL -> 0.5f
                value <= AstronomyService.SUN_MIN_ALTITUDE_ACTUAL -> 0.25f
                else -> 0f
            }
            table.alpha[i] = colorMap.getColor(pct).alpha.toByte()
        }
        return table
    }

    companion object {
        const val SOURCE_ID = "night"
        const val SMOOTH = "smooth"
        const val DEFAULT_SMOOTH = false
    }

}
