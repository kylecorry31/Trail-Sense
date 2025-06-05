package com.kylecorry.trail_sense.tools.declination

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object DeclinationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.DECLINATION,
            context.getString(R.string.declination),
            R.drawable.declination,
            R.id.toolDeclinationFragment,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_declination,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
            )
        )
    }
}