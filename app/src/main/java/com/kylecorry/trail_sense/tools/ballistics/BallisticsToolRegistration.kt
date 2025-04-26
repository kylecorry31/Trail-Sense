package com.kylecorry.trail_sense.tools.ballistics

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object BallisticsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.BALLISTICS,
            context.getString(R.string.ballistics),
            R.drawable.ballistics,
            R.id.fragmentToolBallistics,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_ballistics
        )
    }
}