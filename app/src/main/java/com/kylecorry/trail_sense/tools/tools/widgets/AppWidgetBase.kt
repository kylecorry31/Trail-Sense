package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.widgets.AndromedaCoroutineWidget
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

abstract class AppWidgetBase(private val widgetId: String) :
    AndromedaCoroutineWidget(themeToReload = R.style.WidgetTheme) {

    override suspend fun getUpdatedRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val prefs = UserPreferences(context)
        if (prefs.useDynamicColors) {
            Resources.reloadTheme(context, R.style.WidgetTheme)
        } else {
            Resources.reloadTheme(context, R.style.AppTheme)
        }

        val widget = Tools.getWidget(context, widgetId)!!
        return widget.widgetView.getPopulatedView(context)
    }
}