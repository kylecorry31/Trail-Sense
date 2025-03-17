package com.kylecorry.trail_sense.tools.weather.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.charts.PressureChart
import java.time.Duration
import java.time.Instant

class PressureChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val weather = WeatherSubsystem.getInstance(context)
        val prefs = UserPreferences(context)
        val units = prefs.pressureUnits

        val history = weather.getHistory()
        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }
            .map { it.pressureReading() }
            .map { it.copy(value = it.value.convertTo(units)) }


        val views = getView(context)
        onMain {
            val chart = Chart(context)
            val pressureChart = PressureChart(chart)
            pressureChart.plot(displayReadings)

            val text = Views.text(context, context.getString(R.string.pressure)) as TextView
            text.textAlignment = View.TEXT_ALIGNMENT_CENTER
            val layout = Views.linear(listOf(text, chart))

            renderChart(context, views, layout)
        }

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.WEATHER)
        )
        return views
    }
}