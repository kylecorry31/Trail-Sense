package com.kylecorry.trail_sense.tools.weather.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.charts.PressureChart
import java.time.Duration
import java.time.Instant

class PressureChartToolWidgetView : ToolWidgetView {
    protected val LAYOUT = R.layout.widget_chart
    protected val ROOT = R.id.widget_frame
    protected val TITLE_TEXTVIEW = R.id.widget_title
    protected val CHART = R.id.widget_chart

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val weather = WeatherSubsystem.getInstance(context)
        val prefs = UserPreferences(context)

        val history = weather.getHistory()
        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map { it.pressureReading() }


        val bitmap = onMain {
            val chart = Chart(context)
            val pressureChart = PressureChart(chart)
            pressureChart.plot(displayReadings)

            val width = Resources.dp(context, 400f).toInt()
            val height = Resources.dp(context, 200f).toInt()
            Views.renderViewAsBitmap(chart, width, height)
        }

        val views = getView(context)

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)
        views.setImageViewBitmap(CHART, bitmap)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.WEATHER)
        )
        return views
    }

    override fun getView(context: Context): RemoteViews {
        return RemoteViews(context.packageName, LAYOUT)
    }
}