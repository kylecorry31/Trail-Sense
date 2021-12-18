package com.kylecorry.trail_sense.shared.errors

import com.kylecorry.andromeda.core.system.Android

class DeviceDetailsBugReportGenerator : IBugReportGenerator {
    override fun generate(): String {
        val device = "${Android.fullDeviceName} (${Android.model})"
        return "Device: $device"
    }
}