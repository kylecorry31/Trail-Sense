package com.kylecorry.trail_sense.tiles

import android.app.Dialog
import android.os.Build
import com.kylecorry.andromeda.background.services.AndromedaTileService
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun AndromedaTileService.isForegroundWorkaroundNeeded(): Boolean {
    // The bug only happens on Android 14+
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return false
    }

    // TODO: Only needed if the app is in the background
    return !Permissions.isIgnoringBatteryOptimizations(this)
}

inline fun AndromedaTileService.startWorkaround(crossinline action: suspend () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val dialog = Dialog(this)
        dialog.setTitle(getString(R.string.loading))
        dialog.setOnShowListener {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    action()
                } finally {
                    onMain {
                        dialog.dismiss()
                    }
                }
            }
        }
        showDialog(dialog)
    }
}

inline fun AndromedaTileService.startForegroundService(crossinline action: suspend () -> Unit) {
    if (isForegroundWorkaroundNeeded()) {
        startWorkaround {
            action()
        }
    } else {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch { action() }
    }
}