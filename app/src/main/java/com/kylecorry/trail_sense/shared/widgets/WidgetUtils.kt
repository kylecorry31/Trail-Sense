package com.kylecorry.trail_sense.shared.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

object WidgetUtils {

    fun triggerUpdate(context: Context, component: Class<out AppWidgetProvider>) {
        val intent = Intent(context, component)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(intent.component)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

}