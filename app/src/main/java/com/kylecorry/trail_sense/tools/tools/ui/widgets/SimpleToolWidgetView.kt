package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import com.kylecorry.trail_sense.R

abstract class SimpleToolWidgetView : ToolWidgetView {
    protected val LAYOUT = R.layout.widget_small_simple
    protected val ROOT = R.id.widget_frame
    protected val TITLE_TEXTVIEW = R.id.widget_title
    protected val SUBTITLE_TEXTVIEW = R.id.widget_subtitle
    protected val ICON_IMAGEVIEW = R.id.widget_icon
    protected val ICON_IMAGEVIEW_TEXT_COLOR = R.id.widget_icon_text_color

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override fun getView(context: Context): RemoteViews {
        return RemoteViews(context.packageName, LAYOUT)
    }
}