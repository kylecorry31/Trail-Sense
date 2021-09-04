package com.kylecorry.trail_sense.tools.battery.domain

import java.time.Instant

data class BatteryReading(val time: Instant, val percent: Float, val capacity: Float, val isCharging: Boolean)