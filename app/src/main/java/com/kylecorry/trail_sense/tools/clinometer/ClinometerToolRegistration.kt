package com.kylecorry.trail_sense.tools.clinometer

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.clinometer.volumeactions.ClinometerLockVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.ui.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.ui.Tools

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
                    { context, isToolOpen -> isToolOpen && UserPreferences(context).clinometer.lockWithVolumeButtons },
                    ::ClinometerLockVolumeAction
                )
            )
        )
    }
}