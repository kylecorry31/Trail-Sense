package com.kylecorry.trail_sense.tools.magnifier

import android.content.Context
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object MagnifierToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.MAGNIFIER,
            context.getString(R.string.tool_magnifier_title),
            R.drawable.ic_magnifier,
            R.id.magnifierFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_magnifier,
            isAvailable = { Camera.hasBackCamera(it) },
            diagnostics = listOf(
                ToolDiagnosticFactory.camera(context)
            )
        )
    }
}
