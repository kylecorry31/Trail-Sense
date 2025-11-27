package com.kylecorry.trail_sense.tools.battery.domain

import android.content.Context

data class SystemBatteryTip(
    val name: String,
    val description: String,
    val batteryUsage: BatteryUsage,
    val manage: ((Context) -> Unit)?
)