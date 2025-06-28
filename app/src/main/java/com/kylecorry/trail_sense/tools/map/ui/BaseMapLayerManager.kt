package com.kylecorry.trail_sense.tools.map.ui

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
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
    private val layer: MapLayer,
) : BaseLayerManager() {
    override fun start() {
        // TODO: Tint/mask support
        layer.sourceSelector = PhotoMapTileSourceSelector(
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
                        Size(2880f, 1440f),
                        null,
                        0,
                        MapProjectionType.CylindricalEquidistant
                    ),
                    isAsset = true,
                    isFullWorld = true // TODO: Derive this using calibration points
                )
            ), 1
        )
    }

    override fun stop() {
        // Do nothing
    }
}