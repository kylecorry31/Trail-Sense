package com.kylecorry.trail_sense.tools.ai_assistant

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object AiAssistantToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.AI_ASSISTANT,
            context.getString(R.string.tool_ai_assistant_title),
            R.drawable.ic_ai_assistant,
            R.id.aiAssistantFragment,
            ToolCategory.Other
        )
    }
}
