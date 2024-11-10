package com.kylecorry.trail_sense.tools.experimentation

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object ExperimentationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.EXPERIMENTATION,
            "Experimentation",
            R.drawable.ic_experimental,
            R.id.experimentationFragment,
            ToolCategory.Other,
            isExperimental = true,
            isAvailable = { isDebug() }
        )
    }
}