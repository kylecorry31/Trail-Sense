package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.andromeda.core.coroutines.onMain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class AndromedaCoroutineWidget(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        try {
            CoroutineScope(dispatcher).launch {
                try {
                    for (appWidgetId in appWidgetIds) {
                        val views = getUpdatedRemoteViews(context, appWidgetManager, appWidgetId)
                        onMain {
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            pendingResult.finish()
        }
    }

    /**
     * Get the updated remote views for the widget. This will be passed into appWidgetManager.updateAppWidget for all instances of the widget.
     * @param context The context
     * @param appWidgetManager The app widget manager
     * @return The updated remote views or null if the widget should not be updated
     */
    protected abstract suspend fun getUpdatedRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): RemoteViews
}