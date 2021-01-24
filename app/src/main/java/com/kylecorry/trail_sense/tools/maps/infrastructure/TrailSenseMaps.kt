package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils


object TrailSenseMaps {

    fun open(context: Context) {
        if (!isInstalled(context)) return
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun navigateTo(context: Context, location: Coordinate){
        if (!isInstalled(context)) return
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.data = Uri.parse("geo:${location.latitude},${location.longitude}")
        context.startActivity(intent)
    }

    fun isInstalled(context: Context): Boolean {
        return PackageUtils.isPackageInstalled(context, PACKAGE)
    }

    private const val PACKAGE = "com.kylecorry.trail_sense_maps"
}