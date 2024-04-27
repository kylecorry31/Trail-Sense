package com.kylecorry.trail_sense.tools.metaldetector

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.ui.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object MetalDetectorToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.METAL_DETECTOR,
            context.getString(R.string.tool_metal_detector_title),
            R.drawable.ic_tool_metal_detector,
            R.id.fragmentToolMetalDetector,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_metal_detector,
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.Normal,
                    { context, isToolOpen -> isToolOpen && UserPreferences(context).metalDetector.isMetalAudioEnabled },
                    ::SystemVolumeAction
                )
            ),
            isAvailable = { SensorService(it).hasCompass() }
        )
    }
}