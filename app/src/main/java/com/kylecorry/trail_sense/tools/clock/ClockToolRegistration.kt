package com.kylecorry.trail_sense.tools.clock

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object ClockToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CLOCK,
            context.getString(R.string.tool_clock_title),
            R.drawable.ic_tool_clock,
            R.id.toolClockFragment,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_clock,
            settingsNavAction = R.id.clockSettingsFragment,
        )
    }
}