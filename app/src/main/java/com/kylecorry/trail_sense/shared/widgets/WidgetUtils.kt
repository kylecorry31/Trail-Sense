package com.kylecorry.trail_sense.shared.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.core.view.drawToBitmap
import com.kylecorry.trail_sense.R

object WidgetUtils {

    fun triggerUpdate(context: Context, component: Class<out AppWidgetProvider>) {
        val intent = Intent(context, component)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(intent.component)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    fun reloadTheme(context: Context) {
        context.theme.applyStyle(R.style.AppTheme, true)
    }

    fun renderViewAsBitmap(view: View, width: Int, height: Int): Bitmap {
        view.layoutParams = ViewGroup.LayoutParams(
            width,
            height
        )
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return view.drawToBitmap()
    }

}