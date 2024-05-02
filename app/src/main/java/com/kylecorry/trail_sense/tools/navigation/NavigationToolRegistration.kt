package com.kylecorry.trail_sense.tools.navigation

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object NavigationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.NAVIGATION,
            context.getString(R.string.navigation),
            R.drawable.ic_compass_icon,
            R.id.action_navigation,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_navigation,
            settingsNavAction = R.id.navigationSettingsFragment,
            diagnostics = listOf(
                ToolDiagnostic.magnetometer,
                ToolDiagnostic.accelerometer,
                ToolDiagnostic.camera,
                ToolDiagnostic.gps,
                ToolDiagnostic.barometer,
                ToolDiagnostic.altimeter
            )
        )
    }
}