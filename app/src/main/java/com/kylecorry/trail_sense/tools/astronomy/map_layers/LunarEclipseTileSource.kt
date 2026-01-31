package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.AlphaColorMap
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileImageUtils
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Instant
import java.time.ZonedDateTime

class LunarEclipseTileSource : TileSource {

    private val colorMap = AlphaColorMap(AppColor.Orange.color, 200)
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

        val resolution = TileImageUtils.getRequiredResolution(tile, 8)
        return TileImageUtils.getSampledImage(
            tile.getBounds(),
            resolution,
            tile.size,
            Bitmap.Config.ARGB_8888,
            padding = 2,
            smoothPixelEdges = true,
            getValues = TileImageUtils.parallelGridEvaluation { lat, lon ->
                getEclipseObscuration(Coordinate(lat, lon), time) ?: 0f
            }
        ) { x, y, getValue ->
            val value = getValue(x, y)
            colorMap.getColor(1 - value)
        }
    }

    private fun getEclipseObscuration(location: Coordinate, time: ZonedDateTime): Float? {
        val eclipse =
            astronomy.getLunarEclipse(location, time.toLocalDate()) ?: return null
        if (time < eclipse.start || time > eclipse.end) {
            return null
        }
        return eclipse.obscuration
    }

}
