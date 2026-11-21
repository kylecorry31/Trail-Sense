package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.luna.concurrency.onMain
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
    }

    protected abstract suspend fun getUpdatedRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): RemoteViews
}
