package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.GPSPowerUsage
import com.kylecorry.trail_sense.shared.data.Identifiable

enum class GPSPowerMode(
    override val id: Long,
    val powerUsage: GPSPowerUsage?
) : Identifiable {
    Inherit(1, null),
    Low(2, GPSPowerUsage.Low),
    Balanced(3, GPSPowerUsage.Balanced),
    High(4, GPSPowerUsage.High)
}
