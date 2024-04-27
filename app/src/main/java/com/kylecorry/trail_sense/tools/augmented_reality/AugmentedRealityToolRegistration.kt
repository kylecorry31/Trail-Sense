package com.kylecorry.trail_sense.tools.augmented_reality

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.Tools

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
            isAvailable = { SensorService(it).hasCompass() }
        )
    }
}