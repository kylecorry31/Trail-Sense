package com.kylecorry.trail_sense.tools.weather.widgets

import android.content.Context
import android.widget.RemoteViews
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.setImageViewResourceAsIcon
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherToolWidgetView : SimpleToolWidgetView() {
    override fun onUpdate(context: Context, views: RemoteViews, commit: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            populateWeatherDetails(context, views)
            onMain {
                commit()
            }
        }
    }

    private suspend fun populateWeatherDetails(context: Context, views: RemoteViews) {
        val weather = WeatherSubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val prefs = UserPreferences(context)
        val current = weather.getWeather()

        views.setTextViewText(TITLE_TEXTVIEW, context.getString(R.string.weather))
        views.setTextViewText(
            SUBTITLE_TEXTVIEW, if (current.observation?.temperature != null) {
                context.getString(
                    R.string.dot_separated_pair,
                    formatter.formatWeather(current.prediction.primaryHourly),
                    formatter.formatTemperature(
                        current.observation.temperature.convertTo(prefs.temperatureUnits)
                    )
                )
            } else {
                formatter.formatWeather(current.prediction.primaryHourly)
            }
        )
        views.setImageViewResourceAsIcon(
            context,
            ICON_IMAGEVIEW,
            formatter.getWeatherImage(current.prediction.primaryHourly)
        )
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.WEATHER)
        )
    }
}