package com.kylecorry.trail_sense.tools.lightning

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object LightningStrikeDistanceToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.LIGHTNING_STRIKE_DISTANCE,
            context.getString(R.string.tool_lightning_title),
            R.drawable.ic_torch_on,
            R.id.fragmentToolLightning,
            ToolCategory.Weather,
            context.getString(R.string.tool_lightning_description),
            guideId = R.raw.guide_tool_lightning_strike_distance
        )
    }
}