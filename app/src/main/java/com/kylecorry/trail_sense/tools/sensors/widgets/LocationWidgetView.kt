package com.kylecorry.trail_sense.tools.sensors.widgets

import android.content.Context
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetHelper
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class LocationWidgetView : ToolWidgetView {

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override suspend fun getPopulatedView(
        context: Context,
        prefs: WidgetPreferences?
    ): RemoteViews {
        val views = getView(context, prefs)
        val formatter = FormatService.getInstance(context)
        val locationSubsystem = LocationSubsystem.getInstance(context)
        val location = locationSubsystem.location

        views.setTextViewText(R.id.widget_title, formatter.formatLocation(location))
        views.setOnClickPendingIntent(
            R.id.widget_frame,
            // While this widget belongs to the sensor tool, it makes more sense to open the navigation tool
            NavigationUtils.toolPendingIntent(context, Tools.NAVIGATION)
        )
        return views
    }

    override fun getView(context: Context, prefs: WidgetPreferences?): RemoteViews {
        return WidgetHelper.createThemedRemoteViews(
            context,
            prefs?.getTheme(),
            R.layout.widget_layout_title_only
        )
    }
}
