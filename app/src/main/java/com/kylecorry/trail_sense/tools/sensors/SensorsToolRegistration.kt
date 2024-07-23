package com.kylecorry.trail_sense.tools.sensors

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object SensorsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SENSORS,
            context.getString(R.string.sensors),
            R.drawable.ic_sensors,
            R.id.sensorDetailsFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_sensors,
            diagnostics = listOf(
                ToolDiagnosticFactory.magnetometer(context),
                ToolDiagnosticFactory.accelerometer(context),
                ToolDiagnosticFactory.gyroscope(context),
                ToolDiagnosticFactory.barometer(context),
                ToolDiagnosticFactory.battery(context),
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
            ).distinctBy { it.id }
        )
    }
}