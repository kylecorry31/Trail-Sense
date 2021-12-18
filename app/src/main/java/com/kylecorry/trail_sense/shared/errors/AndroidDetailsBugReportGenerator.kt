package com.kylecorry.trail_sense.shared.errors

import android.os.Build

class AndroidDetailsBugReportGenerator : IBugReportGenerator {
    override fun generate(): String {
        val androidVersion = Build.VERSION.SDK_INT
        return "Android SDK: $androidVersion"
    }
}