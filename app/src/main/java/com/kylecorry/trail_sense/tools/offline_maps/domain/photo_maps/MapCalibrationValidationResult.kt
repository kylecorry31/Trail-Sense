package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

enum class MapCalibrationValidationResult {
    Uncalibrated,
    Valid,
    SamePixel,
    SameImageAxis,
    SameLocation,
    ImplausibleScale
}
