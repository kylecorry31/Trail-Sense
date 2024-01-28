package com.kylecorry.trail_sense.shared

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class ErrorBannerReason(override val id: Long): Identifiable {
    NoCompass(1),
    NoGPS(2),
    LocationNotSet(3),
    CompassPoor(4),
    GPSTimeout(5)
}