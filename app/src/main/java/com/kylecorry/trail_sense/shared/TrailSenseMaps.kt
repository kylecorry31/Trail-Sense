package com.kylecorry.trail_sense.shared

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils


object TrailSenseMaps {

    fun open(context: Context) {
        if (!isInstalled(context)) return
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun isInstalled(context: Context): Boolean {
        return PackageUtils.isPackageInstalled(context, PACKAGE)
    }

    private const val PACKAGE = "com.kylecorry.trail_sense_maps"
}