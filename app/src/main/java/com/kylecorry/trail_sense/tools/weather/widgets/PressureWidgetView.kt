package com.kylecorry.trail_sense.tools.weather.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

class PressureWidgetView : SimpleToolWidgetView() {

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val views = getView(context)
        val weather = WeatherSubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val prefs = UserPreferences(context)
        val current = weather.getWeather()

        views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.pressure))

        // Display current pressure in the subtitle
        val pressure = current.observation?.pressure
        if (pressure != null) {
            val convertedPressure = pressure.convertTo(prefs.pressureUnits)
            views.setTextViewText(
                SUBTITLE_TEXTVIEW,
                formatter.formatPressure(
                    convertedPressure,
                    Units.getDecimalPlaces(convertedPressure.units)
                )
            )
        } else {
            views.setTextViewText(SUBTITLE_TEXTVIEW, "")
        }

        // Set the tendency icon
        val tendency = current.pressureTendency
        val iconRes = when {
            tendency.amount > 0 -> R.drawable.ic_arrow_up
            tendency.amount < 0 -> R.drawable.ic_arrow_down
            else -> R.drawable.ic_steady_arrow
        }

        views.setImageViewResourceAsIcon(
            context,
            ICON_IMAGEVIEW_TEXT_COLOR,
            iconRes
        )

        views.setViewVisibility(ICON_IMAGEVIEW, View.GONE)
        views.setViewVisibility(ICON_IMAGEVIEW_TEXT_COLOR, View.VISIBLE)

        // Set click action to open weather tool
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.WEATHER)
        )
        return views
    }
}
