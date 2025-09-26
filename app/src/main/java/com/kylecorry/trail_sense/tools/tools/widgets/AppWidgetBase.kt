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
        val view = widget.widgetView.getPopulatedView(context)

        // Apply preferences
        val preferences = WidgetPreferences(context, widget, appWidgetId)
        // TODO: Set the theme instead (set the text color and background color)
        // ?attr/colorSurfaceContainer
        view.setInt(
            R.id.widget_frame,
            "setBackgroundResource",
            if (preferences.getBackgroundColor() == WidgetBackgroundColor.Transparent) android.R.color.transparent else R.drawable.widget_background
        )
        return view
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