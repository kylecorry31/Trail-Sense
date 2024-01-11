package com.kylecorry.trail_sense.tiles

import android.os.Build
import com.kylecorry.andromeda.background.services.AndromedaTileService
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.main.TileActivity

fun AndromedaTileService.isForegroundWorkaroundNeeded(): Boolean {
    // The bug only happens on Android 14+
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return false
    }

    // TODO: Only needed if the app is in the background
    return !Permissions.isIgnoringBatteryOptimizations(this)
}

fun AndromedaTileService.startWorkaround(tileId: Int){
    val pendingIntent = TileActivity.pendingIntent(
        this,
        tileId
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startActivityAndCollapse(pendingIntent)
    }
}