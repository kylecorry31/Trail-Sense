package com.kylecorry.trail_sense.tools.solarpanel

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object SolarPanelAlignerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SOLAR_PANEL_ALIGNER,
            context.getString(R.string.tool_solar_panel_title),
            R.drawable.ic_tool_solar_panel,
            R.id.fragmentToolSolarPanel,
            ToolCategory.Power,
            context.getString(R.string.tool_solar_panel_summary),
            guideId = R.raw.guide_tool_solar_panel_aligner,
            isAvailable = { SensorService(it).hasCompass() },
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.tilt(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id }
        )
    }
}