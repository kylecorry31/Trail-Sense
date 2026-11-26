package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

interface ToolWidgetView {

    fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit)

    suspend fun getPopulatedView(context: Context, prefs: WidgetPreferences?): RemoteViews

    fun getView(context: Context, prefs: WidgetPreferences?): RemoteViews

}
