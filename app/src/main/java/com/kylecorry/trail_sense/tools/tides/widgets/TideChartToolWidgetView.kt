package com.kylecorry.trail_sense.tools.tides.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideChart
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import java.time.Duration
import java.time.Instant

class TideChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getPopulatedView(context: Context, prefs: WidgetPreferences?): RemoteViews {
        val tides = TidesSubsystem.getInstance(context)
        val tide = tides.getNearestTide()
        val currentWaterLevel = tide?.today?.waterLevels?.minByOrNull {
            Duration.between(Instant.now(), it.time).abs()
        }

        val views = getView(context, prefs)
        onMain {
            val chart = Chart(context)
            val tideChart = TideChart(chart)
            if (tide != null) {
                tideChart.plot(
                    tide.today.waterLevels,
                    tide.today.waterLevelRange
                )

                currentWaterLevel?.let {
                    tideChart.highlight(it, tide.today.waterLevelRange)
                }
            }

            val text = Views.text(
                context,
                if (tide == null) context.getString(R.string.no_tides) else tide.table.name
            ) as TextView
            text.setTextColor(Resources.androidTextColorPrimary(context))
            text.textAlignment = View.TEXT_ALIGNMENT_CENTER
            val layout = Views.linear(listOf(text, chart))

            renderChart(context, views, layout)
        }

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.TIDES)
        )
        return views
    }
}