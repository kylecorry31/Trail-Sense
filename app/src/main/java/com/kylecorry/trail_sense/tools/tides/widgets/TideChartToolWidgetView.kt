package com.kylecorry.trail_sense.tools.tides.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideChart
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView
import java.time.Duration
import java.time.Instant

class TideChartToolWidgetView : ToolWidgetView {
    protected val LAYOUT = R.layout.widget_chart
    protected val ROOT = R.id.widget_frame
    protected val TITLE_TEXTVIEW = R.id.widget_title
    protected val CHART = R.id.widget_chart

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val tides = TidesSubsystem.getInstance(context)
        val tide = tides.getNearestTide()
        val currentWaterLevel = tide?.today?.waterLevels?.minByOrNull {
            Duration.between(Instant.now(), it.time).abs()
        }

        val bitmap = onMain {
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
            text.textAlignment = View.TEXT_ALIGNMENT_CENTER
            val layout = Views.linear(listOf(text, chart))

            val width = Resources.dp(context, 400f).toInt()
            val height = Resources.dp(context, 200f).toInt()
            Views.renderViewAsBitmap(layout, width, height)
        }

        val views = getView(context)

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)
        views.setImageViewBitmap(CHART, bitmap)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.TIDES)
        )
        return views
    }

    override fun getView(context: Context): RemoteViews {
        return RemoteViews(context.packageName, LAYOUT)
    }
}