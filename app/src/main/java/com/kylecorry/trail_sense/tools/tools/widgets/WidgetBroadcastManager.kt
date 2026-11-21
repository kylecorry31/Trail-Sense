package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object WidgetBroadcastManager {

    fun setup(context: Context) {
        val allWidgets = Tools.getTools(context, false)
            .flatMap { it.widgets }

        val broadcasts = allWidgets
            .flatMap { it.updateBroadcasts }
            .distinct()

        broadcasts.forEach { broadcastId ->
            Tools.subscribe(broadcastId) { _ ->
                val widgets = Tools.getTools(context).flatMap { it.widgets }

                val widgetsToUpdate = widgets.filter { it.updateBroadcasts.contains(broadcastId) }

                widgetsToUpdate.forEach {
                    Tools.triggerWidgetUpdate(context, it.id)
                }
            }
        }
    }
}
