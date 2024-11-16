package com.kylecorry.trail_sense.tools.sensors.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem.SensorRefreshPolicy
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ElevationWidgetView : SimpleToolWidgetView() {

    override fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            populateElevationDetails(context, views)
            onMain {
                commit()
            }
        }
    }

    private suspend fun populateElevationDetails(context: Context, views: RemoteViews) {
        val formatter = FormatService.getInstance(context)
        val locationSubsystem = LocationSubsystem.getInstance(context)
        val prefs = UserPreferences(context)

        // Check if elevation is stale and attempt to get a new reading
        val isStale = locationSubsystem.elevationAge.toMinutes() > 30
        val elevation =
            if (isStale && Permissions.isBackgroundLocationEnabled(context)) {
                val sensors = SensorSubsystem.getInstance(context)
                sensors.getElevation(SensorRefreshPolicy.Refresh)
            } else {
                locationSubsystem.elevation
            }

        val convertedElevation = elevation.convertTo(prefs.baseDistanceUnits)

        views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.elevation))
        views.setTextViewText(
            SUBTITLE_TEXTVIEW,
            formatter.formatDistance(
                convertedElevation,
                Units.getDecimalPlaces(convertedElevation.units)
            )
        )
        views.setImageViewResourceAsIcon(
            context,
            ICON_IMAGEVIEW_TEXT_COLOR,
            R.drawable.ic_altitude
        )
        views.setViewVisibility(ICON_IMAGEVIEW_TEXT_COLOR, View.VISIBLE)
        views.setViewVisibility(ICON_IMAGEVIEW, View.GONE)
        views.setOnClickPendingIntent(
            ROOT,
            // While this widget belongs to the sensor tool, it makes more sense to open the navigation tool
            NavigationUtils.toolPendingIntent(context, Tools.NAVIGATION)
        )
    }
}
