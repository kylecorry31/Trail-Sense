package com.kylecorry.trail_sense.tools.climate

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.climate.infrastructure.ClimateSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object ClimateToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CLIMATE,
            context.getString(R.string.tool_climate),
            R.drawable.ic_temperature_range,
            R.id.climateFragment,
            ToolCategory.Weather,
            context.getString(R.string.tool_climate_summary),
            guideId = R.raw.guide_tool_climate,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context)
            ).distinctBy { it.id },
            singletons = listOf(
                ClimateSubsystem::getInstance
            )
        )
    }
}