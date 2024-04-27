package com.kylecorry.trail_sense.tools.whitenoise

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.ui.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.ui.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.ui.Tools
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trail_sense.tools.whitenoise.quickactions.QuickActionWhiteNoise

object WhiteNoiseToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.WHITE_NOISE,
            context.getString(R.string.tool_white_noise_title),
            R.drawable.ic_tool_white_noise,
            R.id.fragmentToolWhiteNoise,
            ToolCategory.Other,
            context.getString(R.string.tool_white_noise_summary),
            guideId = R.raw.guide_tool_white_noise,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_WHITE_NOISE,
                    context.getString(R.string.tool_white_noise_title),
                    ::QuickActionWhiteNoise
                )
            ),
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.High,
                    { _, isToolOpen -> isToolOpen || WhiteNoiseService.isRunning },
                    ::SystemVolumeAction
                )
            )
        )
    }
}