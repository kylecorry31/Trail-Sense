package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import android.widget.RemoteViews

interface ToolWidgetView {

    fun onEnabled(context: Context)

    fun onDisabled(context: Context)

    fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit)

}