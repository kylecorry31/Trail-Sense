package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.location.IGPS

fun IGPS.hasFix(): Boolean {
    if (!hasValidReading) {
        return false
    }

    // Satellites are only null when the device doesn't report them
    if (satellites == null) {
        return true
    }

    return (satellites ?: 0) >= 0
}