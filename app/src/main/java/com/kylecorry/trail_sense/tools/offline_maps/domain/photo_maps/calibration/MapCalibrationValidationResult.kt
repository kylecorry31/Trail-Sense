package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration

enum class MapCalibrationValidationResult {
    Uncalibrated,
    Valid,
    SamePixel,
    SameImageAxis,
    SameLocation,
    ImplausibleScale
}
