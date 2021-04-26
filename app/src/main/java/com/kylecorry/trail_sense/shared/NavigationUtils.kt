package com.kylecorry.trail_sense.shared

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import com.kylecorry.trail_sense.R

object NavigationUtils {

    fun pendingIntent(context: Context, @IdRes action: Int, args: Bundle? = null): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(action)
            .setArguments(args)
            .createPendingIntent()
    }

}