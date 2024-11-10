package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools


class AppWidgetMoon : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        val widget = getWidget(context) ?: return
        widget.widget.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        val widget = getWidget(context) ?: return
        widget.widget.onDisabled(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val widget = getWidget(context) ?: return

        val views = RemoteViews(context.packageName, widget.widgetResourceId)

        widget.widget.onUpdate(context, views) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun getWidget(context: Context): ToolWidget? {
        return Tools.getWidget(context, AstronomyToolRegistration.WIDGET_MOON)
    }
}