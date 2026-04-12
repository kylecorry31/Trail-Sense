package com.kylecorry.trail_sense.tools.pedometer.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.views.remote.setImageViewResourceAsIcon
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.PedometerSessionRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import java.time.LocalDate
import java.time.ZoneId

class PedometerToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getPopulatedView(
        context: Context,
        prefs: WidgetPreferences?
    ): RemoteViews {
        val views = getView(context, prefs)
        val formatter = FormatService.getInstance(context)
        val userPrefs = UserPreferences(context)
        val counter = StepCounter(PreferencesSubsystem.getInstance(context).preferences)
        val paceCalc = StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
        val repo = PedometerSessionRepo.getInstance(context)

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        // Always daily: today's completed sessions + live counter
        val start = today.atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val sessions = repo.getRange(start, end)
        val dbSteps = sessions.sumOf { it.steps }
        val dbDist = sessions.sumOf { it.distance.toDouble() }.toFloat()
        val liveSteps = counter.steps
        val liveDist = paceCalc.distance(liveSteps).meters().value
        val totalSteps = dbSteps + liveSteps
        val totalDist = dbDist + liveDist

        val converted = Distance.meters(totalDist)
            .convertTo(userPrefs.baseDistanceUnits)
            .toRelativeDistance()
        val distanceText = formatter.formatDistance(
            converted, Units.getDecimalPlaces(converted.units), false
        )

        // Title: step count
        views.setTextViewText(TITLE_TEXTVIEW, DecimalFormatter.format(totalSteps, 0))

        // Icon
        views.setImageViewResourceAsIcon(context, ICON_IMAGEVIEW_TEXT_COLOR, R.drawable.steps)
        views.setViewVisibility(ICON_IMAGEVIEW_TEXT_COLOR, View.VISIBLE)
        views.setViewVisibility(ICON_IMAGEVIEW, View.GONE)

        // Subtitle: distance
        views.setTextViewText(SUBTITLE_TEXTVIEW, distanceText)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.PEDOMETER)
        )
        return views
    }
}
