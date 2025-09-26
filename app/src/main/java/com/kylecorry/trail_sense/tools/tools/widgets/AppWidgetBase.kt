package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

abstract class AppWidgetBase(private val widgetId: String) :
    AndromedaCoroutineWidget(themeToReload = R.style.WidgetTheme) {

    suspend fun getRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        return getUpdatedRemoteViews(context, AppWidgetManager.getInstance(context), appWidgetId)
    }

    override suspend fun getUpdatedRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): RemoteViews {
        val prefs = UserPreferences(context)
        if (prefs.useDynamicColors) {
            Resources.reloadTheme(context, R.style.WidgetTheme)
        } else {
            Resources.reloadTheme(context, R.style.AppTheme)
        }

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