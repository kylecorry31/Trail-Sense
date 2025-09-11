package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.ReplaceColor
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapMetadata
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector

class BaseMapLayerManager(
    private val context: Context,
    private val layer: TiledMapLayer,
) : BaseLayerManager() {
    override fun start() {
        // TODO: Tint/mask support
        layer.sourceSelector = PhotoMapTileSourceSelector(
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
            maxLayers = 1,
            loadPdfs = false,
            isPixelPerfect = true,
            operations = listOf(
                Conditional(
                    SOURCE_MAP_COLOR_WATER != DESTINATION_MAP_COLOR_WATER,
                    ReplaceColor(
                        SOURCE_MAP_COLOR_WATER,
                        DESTINATION_MAP_COLOR_WATER
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
    }

    override fun stop() {
        // Do nothing
    }

    companion object {
        private val SOURCE_MAP_COLOR_DESERT = Color.rgb(232, 225, 182)
        private val SOURCE_MAP_COLOR_ROCK = Color.rgb(202, 195, 184)
        private val SOURCE_MAP_COLOR_GRASS = Color.rgb(189, 204, 150)
        private val SOURCE_MAP_COLOR_ICE = Color.rgb(245, 244, 242)
        private const val SOURCE_MAP_COLOR_WATER = Color.BLACK

        private val DESTINATION_MAP_COLOR_DESERT = SOURCE_MAP_COLOR_DESERT
        private val DESTINATION_MAP_COLOR_ROCK = SOURCE_MAP_COLOR_ROCK
        private val DESTINATION_MAP_COLOR_GRASS = SOURCE_MAP_COLOR_GRASS
        private val DESTINATION_MAP_COLOR_ICE = SOURCE_MAP_COLOR_ICE
        private val DESTINATION_MAP_COLOR_WATER = "#AAD3DF".toColorInt()
    }
}