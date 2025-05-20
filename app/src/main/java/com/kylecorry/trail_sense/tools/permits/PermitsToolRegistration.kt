package com.kylecorry.trail_sense.tools.permits

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object PermitsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PERMITS,
            context.getString(R.string.permits),
            R.drawable.signature,
            R.id.fragmentToolPermits,
            ToolCategory.Other,
            isExperimental = true,
            isAvailable = { isDebug() }
        )
    }
}