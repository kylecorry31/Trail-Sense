package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.toColorInt
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.ReplaceColor
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapMetadata
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapDecoderCache
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector

class BaseMapTileSource : TileSource {

    private val context = AppServiceRegistry.get<Context>()
    private val decoderCache = PhotoMapDecoderCache()
    private val internalSelector = PhotoMapTileSourceSelector(
        context,
        listOf(
            PhotoMap(
                -1,
                "Land",
                "land.webp",
                MapCalibration(
                    true, true, 0f, listOf(
                        MapCalibrationPoint(
                            Coordinate(-90.0, -180.0),
                            PercentCoordinate(0f, 1f)
                        ),
                        MapCalibrationPoint(
                            Coordinate(90.0, 180.0),
                            PercentCoordinate(1f, 0f)
                        )
                    )
                ),
                MapMetadata(
                    Size(3800f, 1900f),
                    null,
                    0,
                    MapProjectionType.CylindricalEquidistant
                ),
                isAsset = true,
                isFullWorld = true // TODO: Derive this using calibration points
            )
        ),
        decoderCache,
        maxLayers = 1,
        loadPdfs = false,
        isPixelPerfect = true,
        operations = listOf(
            Conditional(
                SOURCE_MAP_COLOR_OCEAN != DESTINATION_MAP_COLOR_OCEAN,
                ReplaceColor(
                    SOURCE_MAP_COLOR_OCEAN,
                    DESTINATION_MAP_COLOR_OCEAN
                )
            ),
            Conditional(
                SOURCE_MAP_COLOR_LAKES != DESTINATION_MAP_COLOR_LAKES,
                ReplaceColor(
                    SOURCE_MAP_COLOR_LAKES,
                    DESTINATION_MAP_COLOR_LAKES
                )
            ),
            Conditional(
                SOURCE_MAP_COLOR_DESERT != DESTINATION_MAP_COLOR_DESERT,
                ReplaceColor(
                    SOURCE_MAP_COLOR_DESERT,
                    DESTINATION_MAP_COLOR_DESERT
                )
            ),
            Conditional(
                SOURCE_MAP_COLOR_ROCK != DESTINATION_MAP_COLOR_ROCK,
                ReplaceColor(
                    SOURCE_MAP_COLOR_ROCK,
                    DESTINATION_MAP_COLOR_ROCK
                )
            ),
            Conditional(
                SOURCE_MAP_COLOR_GRASS != DESTINATION_MAP_COLOR_GRASS,
                ReplaceColor(
                    SOURCE_MAP_COLOR_GRASS,
                    DESTINATION_MAP_COLOR_GRASS
                )
            ),
            Conditional(
                SOURCE_MAP_COLOR_ICE != DESTINATION_MAP_COLOR_ICE,
                ReplaceColor(
                    SOURCE_MAP_COLOR_ICE,
                    DESTINATION_MAP_COLOR_ICE
                )
            )
        )
    )

    override suspend fun loadTile(tile: Tile, params: Bundle): Bitmap? {
        return internalSelector.loadTile(tile, params)
    }

    override suspend fun cleanup() {
        decoderCache.recycleInactive(emptyList())
    }

    companion object {
        const val SOURCE_ID = "base_map"
        private val SOURCE_MAP_COLOR_DESERT = Color.rgb(232, 225, 182)
        private val SOURCE_MAP_COLOR_ROCK = Color.rgb(202, 195, 184)
        private val SOURCE_MAP_COLOR_GRASS = Color.rgb(189, 204, 150)
        private val SOURCE_MAP_COLOR_ICE = Color.rgb(245, 244, 242)
        private const val SOURCE_MAP_COLOR_OCEAN = Color.BLACK
        private val SOURCE_MAP_COLOR_LAKES = Color.rgb(127, 127, 127)

        private val DESTINATION_MAP_COLOR_DESERT = SOURCE_MAP_COLOR_DESERT
        private val DESTINATION_MAP_COLOR_ROCK = SOURCE_MAP_COLOR_ROCK
        private val DESTINATION_MAP_COLOR_GRASS = SOURCE_MAP_COLOR_GRASS
        private val DESTINATION_MAP_COLOR_ICE = SOURCE_MAP_COLOR_ICE
        private val DESTINATION_MAP_COLOR_OCEAN = "#AAD3DF".toColorInt()
        private val DESTINATION_MAP_COLOR_LAKES = "#AAD3DF".toColorInt()
    }
}
