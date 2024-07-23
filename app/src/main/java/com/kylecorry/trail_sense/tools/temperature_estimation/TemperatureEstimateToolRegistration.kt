package com.kylecorry.trail_sense.tools.temperature_estimation

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object TemperatureEstimateToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.TEMPERATURE_ESTIMATION,
            context.getString(R.string.tool_temperature_estimation_title),
            R.drawable.thermometer,
            R.id.temperatureEstimationFragment,
            ToolCategory.Weather,
            context.getString(R.string.tool_temperature_estimation_description),
            guideId = R.raw.guide_tool_temperature_estimation,
            diagnostics = listOf(
                *ToolDiagnosticFactory.altimeter(context),
            ).distinctBy { it.id }
        )
    }
}