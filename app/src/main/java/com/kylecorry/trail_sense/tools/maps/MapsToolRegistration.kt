package com.kylecorry.trail_sense.tools.maps

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object MapsToolRegistration : ToolRegistration {

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.MAPS,
            context.getString(R.string.maps),
            R.drawable.maps,
            R.id.mapsFragment,
            ToolCategory.Location,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id },
        )
    }
}