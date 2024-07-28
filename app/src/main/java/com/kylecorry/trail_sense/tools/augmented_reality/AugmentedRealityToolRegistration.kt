package com.kylecorry.trail_sense.tools.augmented_reality

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object AugmentedRealityToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.AUGMENTED_REALITY,
            context.getString(R.string.augmented_reality),
            R.drawable.ic_camera,
            R.id.augmentedRealityFragment,
            ToolCategory.Other,
            context.getString(R.string.augmented_reality_description),
            guideId = R.raw.guide_tool_augmented_reality,
            settingsNavAction = R.id.augmentedRealitySettingsFragment,
            isAvailable = { SensorService(it).hasCompass() },
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.Normal,
                    { _, isOpen, _ -> isOpen },
                    ::SystemVolumeAction
                )
            ),
            diagnostics = listOf(
                *ToolDiagnosticFactory.sightingCompass(context),
                *ToolDiagnosticFactory.altimeter(context),
                ToolDiagnosticFactory.gps(context),
            ).distinctBy { it.id }
        )
    }
}