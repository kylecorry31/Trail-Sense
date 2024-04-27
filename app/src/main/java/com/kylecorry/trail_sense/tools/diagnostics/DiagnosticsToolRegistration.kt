package com.kylecorry.trail_sense.tools.diagnostics

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object DiagnosticsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.DIAGNOSTICS,
            context.getString(R.string.diagnostics),
            R.drawable.ic_alert,
            R.id.diagnosticsFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_diagnostics
        )
    }
}