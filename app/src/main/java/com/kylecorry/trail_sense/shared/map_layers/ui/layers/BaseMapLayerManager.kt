package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.bitmaps.ReplaceColor
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibration
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapMetadata
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.BaseLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MapLayer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector

class BaseMapLayerManager(
    private val context: Context,
    private val layer: MapLayer,
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
                // TODO: Allow the user to replace the land color
//                Threshold(10f),
//                ReplaceColor(Color.WHITE, "#7DBA4E".toColorInt()),
                // TODO: Allow the user to replace the water color
                // Replace the water color
                ReplaceColor(Color.BLACK, "#4A90E2".toColorInt())
            )
        )
    }

    override fun stop() {
        // Do nothing
    }
}