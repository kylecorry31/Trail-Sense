package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChart
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView
import java.time.Duration
import java.time.Instant

class SunAndMoonChartToolWidgetView : ToolWidgetView {
    protected val LAYOUT = R.layout.widget_chart
    protected val ROOT = R.id.widget_frame
    protected val TITLE_TEXTVIEW = R.id.widget_title
    protected val CHART = R.id.widget_chart

    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override suspend fun getPopulatedView(context: Context): RemoteViews {
        val astronomy = AstronomySubsystem.getInstance(context)

        val moon = astronomy.moon

        val instant = Instant.now()
        val sunAltitudes = astronomy.getSunAltitudes()
        val moonAltitudes = astronomy.getMoonAltitudes()
        val currentSun = sunAltitudes.minByOrNull {
            Duration.between(instant, it.time).abs()
        }
        val currentMoon = moonAltitudes.minByOrNull {
            Duration.between(instant, it.time).abs()
        }

        val bitmap = onMain {
            val chart = Chart(context)
            val astroChart = AstroChart(chart) {}
            astroChart.setMoonImage(R.drawable.ic_moon)
            astroChart.plot(sunAltitudes, moonAltitudes)

            val moonImage = MoonPhaseImageMapper().getPhaseImage(moon.phase)

            astroChart.setMoonImage(moonImage)
            astroChart.moveSun(currentSun)
            astroChart.moveMoon(currentMoon, moon.tilt)

            val width = Resources.dp(context, 400f).toInt()
            val height = Resources.dp(context, 200f).toInt()
            Views.renderViewAsBitmap(chart, width, height)
        }

        val views = getView(context)

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)
        views.setImageViewBitmap(CHART, bitmap)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.ASTRONOMY)
        )
        return views
    }

    override fun getView(context: Context): RemoteViews {
        return RemoteViews(context.packageName, LAYOUT)
    }
}