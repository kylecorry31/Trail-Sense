package com.kylecorry.trail_sense.tools.waterpurification

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object WaterBoilTimerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.WATER_BOIL_TIMER,
            context.getString(R.string.water_boil_timer),
            R.drawable.ic_tool_boil_done,
            R.id.waterPurificationFragment,
            ToolCategory.Time,
            context.getString(R.string.tool_boil_summary),
            guideId = R.raw.guide_tool_water_boil_timer
        )
    }
}