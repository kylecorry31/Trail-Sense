package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools


abstract class AppWidgetBase(private val widgetId: String) : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val widget = getWidget(context) ?: return

        val pendingResult = goAsync()
        try {
            val views = RemoteViews(context.packageName, widget.widgetResourceId)

            Log.d("Widget", "Updating widget $widgetId")
            widget.widgetView.onUpdate(context, views)
            {
                for (appWidgetId in appWidgetIds) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                Log.d("Widget", "Finished updating widget $widgetId")
                pendingResult.finish()
            }
        } catch (e: Exception) {
            Log.e("Widget", "Error updating widget $widgetId", e)
            pendingResult.finish()
        }
    }

    override fun onEnabled(context: Context) {
        val widget = getWidget(context) ?: return
        widget.widgetView.onEnabled(context)
        Log.d("Widget", "Enabled widget $widgetId")
    }

    override fun onDisabled(context: Context) {
        val widget = getWidget(context) ?: return
        widget.widgetView.onDisabled(context)
        Log.d("Widget", "Disabled widget $widgetId")
    }

    private fun getWidget(context: Context): ToolWidget? {
        return Tools.getWidget(context, widgetId)
    }
}