package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object WidgetBroadcastManager {

    fun setup(context: Context) {
        val widgets = Tools.getTools(context, false)
            .flatMap { it.widgets }

        // Get all unique broadcast IDs
        val broadcasts = widgets
            .flatMap { it.updateBroadcasts }
            .distinct()

        // Subscribe to each broadcast
        broadcasts.forEach { broadcastId ->
            Tools.subscribe(broadcastId) { _ ->
                // Find widgets that listen to this broadcast
                val widgetsToUpdate = widgets.filter { it.updateBroadcasts.contains(broadcastId) }
                
                // Update each widget
                widgetsToUpdate.forEach { widget ->
                    Tools.triggerWidgetUpdate(context, widget.id)
                }
                true
            }
        }
    }
}