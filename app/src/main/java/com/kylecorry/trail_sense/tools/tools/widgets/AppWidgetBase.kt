package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.andromeda.widgets.AndromedaCoroutineWidget
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

abstract class AppWidgetBase(private val widgetId: String) :
    AndromedaCoroutineWidget(themeToReload = R.style.AppTheme) {

    override suspend fun getUpdatedRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val widget = Tools.getWidget(context, widgetId)!!
        return widget.widgetView.getPopulatedView(context)
    }
}