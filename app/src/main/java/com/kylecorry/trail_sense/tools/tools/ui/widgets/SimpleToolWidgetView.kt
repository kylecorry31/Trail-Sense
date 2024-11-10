package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class SimpleToolWidgetView : ToolWidgetView {
    protected val ROOT = R.id.widget_frame
    protected val TITLE_TEXTVIEW = R.id.widget_title
    protected val SUBTITLE_TEXTVIEW = R.id.widget_subtitle
    protected val ICON_IMAGEVIEW = R.id.widget_icon

    protected var scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onEnabled(context: Context) {
        scope = CoroutineScope(Dispatchers.Default)
    }

    override fun onDisabled(context: Context) {
        scope.cancel()
    }
}