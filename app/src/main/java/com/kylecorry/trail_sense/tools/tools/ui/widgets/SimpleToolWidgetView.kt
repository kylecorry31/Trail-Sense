package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import com.kylecorry.trail_sense.R

abstract class SimpleToolWidgetView : ToolWidgetView {
    protected val ROOT = R.id.widget_frame
    protected val TITLE_TEXTVIEW = R.id.widget_title
    protected val SUBTITLE_TEXTVIEW = R.id.widget_subtitle
    protected val ICON_IMAGEVIEW = R.id.widget_icon

    override fun onEnabled(context: Context) {
        // Do nothing
    }

    override fun onDisabled(context: Context) {
        // Do nothing
    }
}