package com.kylecorry.trail_sense.tools.tides

import android.content.Context
import com.kylecorry.trail_sense.R
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
                    R.layout.widget_small_simple,
                    TidesToolWidgetView()
                )
            )
        )
    }

    const val WIDGET_TIDES = "tides_widget_tides"
}