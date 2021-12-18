package com.kylecorry.trail_sense.shared.errors

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.BuildConfig

class AppDetailsBugReportGenerator(private val context: Context) : IBugReportGenerator {
    override fun generate(): String {
        val appVersion = Package.getVersionName(context)
        val isDebug = BuildConfig.DEBUG
        return "Trail Sense: $appVersion${if (isDebug) " (Debug)" else ""}"
    }
}