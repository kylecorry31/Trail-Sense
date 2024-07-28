package com.kylecorry.trail_sense.tools.whistle

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.whistle.quickactions.QuickActionWhistle

object WhistleToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.WHISTLE,
            context.getString(R.string.tool_whistle_title),
            R.drawable.ic_tool_whistle,
            R.id.toolWhistleFragment,
            ToolCategory.Signaling,
            guideId = R.raw.guide_tool_whistle,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_WHISTLE,
                    context.getString(R.string.tool_whistle_title),
                    ::QuickActionWhistle
                )
            ),
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.High,
                    { _, isToolOpen, _ -> isToolOpen || QuickActionWhistle.isRunning },
                    ::SystemVolumeAction
                )
            )
        )
    }
}