package com.kylecorry.trail_sense.shared.navigation

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUiSaveStateControl
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object NavigationUtils {

    fun pendingIntent(context: Context, @IdRes action: Int, args: Bundle? = null): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(action)
            .setArguments(args)
            .createPendingIntent()
    }

    fun toolPendingIntent(context: Context, toolId: Long, args: Bundle? = null): PendingIntent {
        val tool = Tools.getTool(context, toolId)!!
        return pendingIntent(context, tool.navAction, args)
    }

    @OptIn(NavigationUiSaveStateControl::class)
    fun BottomNavigationView.setupWithNavController(
        navController: NavController,
        saveState: Boolean
    ) {
        NavigationUI.setupWithNavController(this, navController, saveState)
    }

}