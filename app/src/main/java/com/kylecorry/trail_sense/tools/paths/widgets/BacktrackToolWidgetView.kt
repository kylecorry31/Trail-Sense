package com.kylecorry.trail_sense.tools.paths.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView

class BacktrackToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val views = getView(context)
        val pathService = PathService.getInstance(context)
        val isBacktrackActive =
            Tools.getService(context, PathsToolRegistration.SERVICE_BACKTRACK)?.isEnabled() == true
        val backtrackPathId = pathService.getBacktrackPathId()
        val path = backtrackPathId?.let { pathService.getPath(it) }
        val distance = path?.metadata?.distance ?: Distance.meters(0f)
        val prefs = UserPreferences(context)
        val units = prefs.baseDistanceUnits
        val convertedDistance = distance.convertTo(units).toRelativeDistance()
        val formatter = FormatService.getInstance(context)
        val formattedDistance = formatter.formatDistance(
            convertedDistance,
            Units.getDecimalPlaces(convertedDistance.units),
        )

        views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.backtrack))
        views.setViewVisibility(ICON_IMAGEVIEW, View.GONE)
        views.setViewVisibility(ICON_IMAGEVIEW_TEXT_COLOR, View.VISIBLE)
        views.setImageViewResource(ICON_IMAGEVIEW_TEXT_COLOR, R.drawable.ic_tool_backtrack)
        views.setTextViewText(
            SUBTITLE_TEXTVIEW, if (isBacktrackActive) {
                formattedDistance
            } else {
                context.getString(R.string.off)
            }
        )
        views.setOnClickPendingIntent(ROOT, NavigationUtils.toolPendingIntent(context, Tools.PATHS))
        return views
    }
}
