package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.exceptions.IBugReportGenerator

class PackageNameBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        return Package.getPackageName(context)
    }
}