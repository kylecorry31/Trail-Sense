package com.kylecorry.trail_sense.shared.system

import android.content.Context

object PackageUtils {

    fun getPackageName(context: Context): String {
        return context.packageName
    }

    fun getVersionName(context: Context): String {
        val packageManager = context.packageManager
        return packageManager.getPackageInfo(getPackageName(context), 0).versionName
    }

}