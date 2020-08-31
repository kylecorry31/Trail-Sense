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

    fun getVersionCode(context: Context): Long {
        val packageManager = context.packageManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(getPackageName(context), 0).longVersionCode
        } else {
            packageManager.getPackageInfo(getPackageName(context), 0).versionCode.toLong()
        }
    }

}