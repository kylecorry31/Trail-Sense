package com.kylecorry.trail_sense.shared

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavDeepLinkBuilder
import com.kylecorry.trail_sense.R

object NavigationUtils {

    fun pendingIntent(context: Context, @IdRes action: Int, args: Bundle? = null): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(action)
            .setArguments(args)
            .createTaskStackBuilder()
            .getPendingIntent(
                getRequestCode(action, args),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
    }

    private fun getRequestCode(destId: Int, args: Bundle?): Int {
        // Taken from NavDeepLinkBuilder - can be removed once version 2.4.X is used
        var requestCode = 0
        if (args != null) {
            for (key in args.keySet()) {
                val value = args.get(key)
                requestCode = 31 * requestCode + (value?.hashCode() ?: 0)
            }
        }
        return 31 * requestCode + destId
    }

}