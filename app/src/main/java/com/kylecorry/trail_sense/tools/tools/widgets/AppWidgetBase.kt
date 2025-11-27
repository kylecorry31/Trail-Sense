package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

abstract class AppWidgetBase(private val widgetId: String) : AndromedaCoroutineWidget() {

    suspend fun getRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        return getUpdatedRemoteViews(context, AppWidgetManager.getInstance(context), appWidgetId)
    }

    override suspend fun getUpdatedRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): RemoteViews {
        val widget = Tools.getWidget(context, widgetId)!!
        val preferences = WidgetPreferences(context, widget, appWidgetId)
        return widget.widgetView.getPopulatedView(context, preferences)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val widget = Tools.getWidget(context, widgetId)!!
        appWidgetIds.forEach {
            val widgetPrefs = WidgetPreferences(context, widget, it)
            widgetPrefs.clear()
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        suspend fun forceUpdate(context: Context, widgetProvider: AppWidgetBase, appWidgetId: Int) {
            val view = onDefault { widgetProvider.getRemoteViews(context, appWidgetId) }
            onMain {
                val manager = AppWidgetManager.getInstance(context)
                manager.updateAppWidget(appWidgetId, view)
            }
        }
    }
}