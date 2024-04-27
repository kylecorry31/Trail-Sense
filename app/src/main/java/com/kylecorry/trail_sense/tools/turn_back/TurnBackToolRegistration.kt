package com.kylecorry.trail_sense.tools.turn_back

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object TurnBackToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.TURN_BACK,
            context.getString(R.string.tool_turn_back),
            R.drawable.ic_undo,
            R.id.turnBackFragment,
            ToolCategory.Time
        )
    }
}