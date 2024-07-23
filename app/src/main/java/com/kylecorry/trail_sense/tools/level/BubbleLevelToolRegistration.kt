package com.kylecorry.trail_sense.tools.level

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object BubbleLevelToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.BUBBLE_LEVEL,
            context.getString(R.string.tool_bubble_level_title),
            R.drawable.level,
            R.id.levelFragment,
            ToolCategory.Angles,
            context.getString(R.string.tool_bubble_level_summary),
            guideId = R.raw.guide_tool_bubble_level,
            diagnostics = listOf(
                *ToolDiagnosticFactory.tilt(context)
            ).distinctBy { it.id }
        )
    }
}