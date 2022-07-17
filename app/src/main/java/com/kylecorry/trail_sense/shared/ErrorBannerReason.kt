package com.kylecorry.trail_sense.shared

import com.kylecorry.trail_sense.shared.database.Identifiable

enum class ErrorBannerReason(override val id: Long): Identifiable {
    NoGPS(1),
    LocationNotSet(2),
    CompassPoor(3),
    NoCompass(4),
    GPSTimeout(5)
}