package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.andromeda.core.ui.colormaps.AlphaColorMap
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.map_layers.tiles.ParallelCoordinateGridValueProvider
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileImageUtils
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class LunarEclipseTileSource : TileSource {

    private val colorMap = AlphaColorMap(AppColor.Orange.color, 200)
    private val astronomy = AstronomyService()

    override suspend fun loadTile(tile: Tile, params: Bundle): Bitmap? {
        val preferences = params.getBundle(TileSource.PARAM_PREFERENCES)
        val showPath =
            preferences?.getBoolean(LunarEclipseLayer.SHOW_PATH, LunarEclipseLayer.DEFAULT_SHOW_PATH)
                ?: LunarEclipseLayer.DEFAULT_SHOW_PATH

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
            getEclipseObscuration(it, time, showPath) != null
        }

        if (!isEclipseVisible) {
            return null
        }

        val resolution = TileImageUtils.getRequiredResolution(tile, 8)
        return TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            smoothPixelEdges = true,
            valueProvider = ParallelCoordinateGridValueProvider { lat, lon ->
                getEclipseObscuration(Coordinate(lat, lon), time, showPath) ?: 0f
            }
        ) { x, y, getValue ->
            val value = getValue(x, y)
            colorMap.getColor(value)
        }
    }

    private fun getEclipseObscuration(
        location: Coordinate,
        time: ZonedDateTime,
        showPath: Boolean
    ): Float? {
        val eclipse =
            astronomy.getLunarEclipse(location, time.toLocalDate()) ?: astronomy.getLunarEclipse(
                location,
                time.toLocalDate().plusDays(1)
            ) ?: return null

        val buffer = if (showPath) Duration.ofHours(8) else Duration.ZERO

        if (time < eclipse.start.minus(buffer) || time > eclipse.end.plus(buffer)) {
            return null
        }
        return eclipse.obscuration
    }

}
