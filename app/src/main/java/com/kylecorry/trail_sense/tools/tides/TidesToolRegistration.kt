package com.kylecorry.trail_sense.tools.tides

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tides.widgets.AppWidgetTideChart
import com.kylecorry.trail_sense.tools.tides.widgets.AppWidgetTides
import com.kylecorry.trail_sense.tools.tides.widgets.TideChartToolWidgetView
import com.kylecorry.trail_sense.tools.tides.widgets.TidesToolWidgetView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object TidesToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.TIDES,
            context.getString(R.string.tides),
            R.drawable.ic_tide_table,
            R.id.tidesFragment,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_tides,
            settingsNavAction = R.id.tideSettingsFragment,
            additionalNavigationIds = listOf(
                R.id.tideListFragment,
                R.id.createTideFragment
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context)
            ),
            widgets = listOf(
                ToolWidget(
                    WIDGET_TIDES,
                    context.getString(R.string.tides),
                    ToolSummarySize.Half,
                    TidesToolWidgetView(),
                    AppWidgetTides::class.java,
                    usesLocation = true
                ),
                ToolWidget(
                    WIDGET_TIDE_CHART,
                    context.getString(R.string.tide_chart),
                    ToolSummarySize.Full,
                    TideChartToolWidgetView(),
                    AppWidgetTideChart::class.java,
                    usesLocation = true
                ),
            )
        )
    }

    const val WIDGET_TIDES = "tides-widget-tides"
    const val WIDGET_TIDE_CHART = "tides-widget-tide-chart"
}