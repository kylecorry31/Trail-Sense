package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView

abstract class ChartToolWidgetViewBase : ToolWidgetView {
    private val LAYOUT = R.layout.widget_chart
    private val TRANSPARENT_BLACK_LAYOUT = R.layout.widget_transparent_black_chart
    private val TRANSPARENT_WHITE_LAYOUT = R.layout.widget_transparent_white_chart
    protected val ROOT = R.id.widget_frame
    protected val TITLE_TEXTVIEW = R.id.widget_title
    protected val CHART = R.id.widget_chart

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    protected fun renderChart(context: Context, views: RemoteViews, view: View) {
        val width = Resources.dp(context, 400f).toInt()
        val height = Resources.dp(context, 200f).toInt()
        val bitmap = Views.renderViewAsBitmap(view, width, height)
        views.setImageViewBitmap(CHART, bitmap)
    }

    override fun getView(context: Context, prefs: WidgetPreferences?): RemoteViews {
        val theme = prefs?.getTheme()
        if (theme?.themeId != null) {
            Resources.reloadTheme(context, theme.themeId)
        }
        return RemoteViews(
            context.packageName, when (theme) {
                WidgetTheme.TransparentBlack -> TRANSPARENT_BLACK_LAYOUT
                WidgetTheme.TransparentWhite -> TRANSPARENT_WHITE_LAYOUT
                else -> LAYOUT
            }
        )
    }
}