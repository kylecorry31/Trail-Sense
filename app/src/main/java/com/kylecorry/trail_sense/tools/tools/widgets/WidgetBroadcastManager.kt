package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object WidgetBroadcastManager {

    fun setup(context: Context) {
        val allWidgets = Tools.getTools(context, false)
            .flatMap { it.widgets }

        // Get all unique broadcast IDs
        val broadcasts = allWidgets
            .flatMap { it.updateBroadcasts }
            .distinct()

        // Subscribe to each broadcast
        broadcasts.forEach { broadcastId ->
            Tools.subscribe(broadcastId) { _ ->
                val widgets = Tools.getTools(context).flatMap { it.widgets }

                // Find widgets that listen to this broadcast
                val widgetsToUpdate = widgets.filter { it.updateBroadcasts.contains(broadcastId) }

                // Update each widget
                widgetsToUpdate.forEach {
                    Tools.triggerWidgetUpdate(context, it.id)
                }
                true
            }
        }
    }
}