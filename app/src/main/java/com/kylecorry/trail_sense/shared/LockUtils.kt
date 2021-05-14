package com.kylecorry.trail_sense.shared

import android.app.Activity
import android.os.Build
import android.view.WindowManager

// TODO: Move this into TS Core
object LockUtils {

    @Suppress("DEPRECATION")
    fun setShowWhenLocked(activity: Activity, showWhenLocked: Boolean){
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                activity.setShowWhenLocked(showWhenLocked)
            }
            showWhenLocked -> {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }
            else -> {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        }
    }

}