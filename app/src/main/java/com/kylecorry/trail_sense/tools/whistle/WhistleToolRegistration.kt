package com.kylecorry.trail_sense.tools.whistle

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
                    ToolVolumeActionPriority.Normal,
                    { _, isToolOpen -> isToolOpen },
                    ::SystemVolumeAction
                )
            )
        )
    }
}