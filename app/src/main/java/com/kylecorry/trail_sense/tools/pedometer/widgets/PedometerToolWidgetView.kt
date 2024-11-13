package com.kylecorry.trail_sense.tools.pedometer.widgets

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.jvm.optionals.getOrNull

class PedometerToolWidgetView : SimpleToolWidgetView() {

    override fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            populatePedometerDetails(context, views)
            onMain {
                commit()
            }
        }
    }

    private fun populatePedometerDetails(context: Context, views: RemoteViews) {
        val subsystem = PedometerSubsystem.getInstance(context)
        val prefs = UserPreferences(context)
        val formatter = FormatService.getInstance(context)
        val steps = subsystem.steps.value.getOrNull() ?: 0
        val distance = subsystem.distance.value.getOrNull() ?: Distance.meters(0f)
        val converted = distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val formattedDistance =
            formatter.formatDistance(converted, Units.getDecimalPlaces(converted.units))

        views.setTextViewText(TITLE_TEXTVIEW, formattedDistance)
        val icon = Icon.createWithResource(context, R.drawable.steps)
        views.setViewVisibility(ICON_IMAGEVIEW, android.view.View.GONE)
        views.setViewVisibility(ICON_IMAGEVIEW_TEXT_COLOR, android.view.View.VISIBLE)
        views.setImageViewIcon(ICON_IMAGEVIEW_TEXT_COLOR, icon)
        views.setTextViewText(
            SUBTITLE_TEXTVIEW,
            context.resources.getQuantityString(
                R.plurals.number_steps,
                steps.toInt(),
                steps.toInt()
            )
        )
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.PEDOMETER)
        )
    }
}