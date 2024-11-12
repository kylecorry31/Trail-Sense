package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle

interface ToolWidgetView {

    fun onEnabled(context: Context)

    fun onDisabled(context: Context)

    fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit)

    fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit)

}