package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChart
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import java.time.Duration
import java.time.Instant

class SunAndMoonChartToolWidgetView : ChartToolWidgetViewBase() {

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

        val views = getView(context)
        onMain {
            val chart = Chart(context)
            val astroChart = AstroChart(chart) {}
            astroChart.setMoonImage(R.drawable.ic_moon)
            astroChart.plot(sunAltitudes, moonAltitudes)

            val moonImage = MoonPhaseImageMapper().getPhaseImage(moon.phase)

            astroChart.setMoonImage(moonImage)
            astroChart.moveSun(currentSun)
            astroChart.moveMoon(currentMoon, moon.tilt)

            renderChart(context, views, chart)
        }

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)

        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.ASTRONOMY)
        )
        return views
    }
}