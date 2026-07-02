package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration

import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.sol.units.Coordinate

data class MapCalibrationPoint(val location: Coordinate, val imageLocation: PercentCoordinate)
