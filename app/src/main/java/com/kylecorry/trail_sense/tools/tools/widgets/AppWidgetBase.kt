package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


abstract class AppWidgetBase(private val widgetId: String) : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val widget = getWidget(context) ?: return

        val pendingResult = goAsync()
        try {
            Log.d("Widget", "Updating widget $widgetId")
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val views = widget.widgetView.getPopulatedView(context)
                    onMain {
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
        } catch (e: Exception) {
            Log.e("Widget", "Error updating widget $widgetId", e)
            pendingResult.finish()
        }
    }

    private fun getWidget(context: Context): ToolWidget? {
        return Tools.getWidget(context, widgetId)
    }
}