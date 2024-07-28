package com.kylecorry.trail_sense.tools.clinometer

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.clinometer.volumeactions.ClinometerLockVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object ClinometerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CLINOMETER,
            context.getString(R.string.clinometer_title),
            R.drawable.clinometer,
            R.id.clinometerFragment,
            ToolCategory.Angles,
            context.getString(R.string.tool_clinometer_summary),
            guideId = R.raw.guide_tool_clinometer,
            settingsNavAction = R.id.clinometerSettingsFragment,
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.Normal,
                    { context, isToolOpen, _ -> isToolOpen && UserPreferences(context).clinometer.lockWithVolumeButtons },
                    ::ClinometerLockVolumeAction
                )
            ),
            diagnostics = listOf(
                *ToolDiagnosticFactory.tilt(context),
                ToolDiagnosticFactory.camera(context)
            ).distinctBy { it.id }
        )
    }
}