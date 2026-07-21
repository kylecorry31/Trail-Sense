package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.AstroChart
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import java.time.Duration
import java.time.Instant

class SunAndMoonChartToolWidgetView : ChartToolWidgetViewBase() {

    override suspend fun getPopulatedView(
        context: Context,
        prefs: WidgetPreferences?
    ): RemoteViews {
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

        val views = getView(context, prefs)
        onMain {
            val chart = Chart(context)
            val userPrefs = UserPreferences(context)
            val astroChart = AstroChart(chart) {}
            astroChart.setBands(userPrefs.astronomy.showAstronomyBands)
            astroChart.plot(sunAltitudes, moonAltitudes)

            val size = Resources.dp(context, 24f).toInt()
            val moonImage =
                MoonPhaseImageMapper(context).getPhaseImage(moon.phaseAngle, size, size, moon.tilt)

            astroChart.setMoonImage(moonImage)
            astroChart.moveSun(currentSun)
            astroChart.moveMoon(currentMoon)

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
