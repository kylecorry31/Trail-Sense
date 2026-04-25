package com.kylecorry.trail_sense.tools.photo_maps.domain

enum class MapCalibrationValidationResult {
    Uncalibrated,
    Valid,
    SamePixel,
    SameImageAxis,
    SameLocation,
    ImplausibleScale
}
