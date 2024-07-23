package com.kylecorry.trail_sense.tools.triangulate

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object TriangulateLocationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.TRIANGULATE_LOCATION,
            context.getString(R.string.tool_triangulate_title),
            R.drawable.ic_tool_triangulate,
            R.id.fragmentToolTriangulate,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_triangulate_location,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.sightingCompass(context)
            ).distinctBy { it.id }
        )
    }
}