package com.kylecorry.trail_sense.tools.field_guide

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object FieldGuideToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.FIELD_GUIDE,
            context.getString(R.string.field_guide),
            R.drawable.field_guide,
            R.id.fieldGuideFragment,
            ToolCategory.Books,
            additionalNavigationIds = listOf(R.id.fieldGuidePageFragment),
            guideId = R.raw.guide_tool_field_guide,
            singletons = listOf(
                FieldGuideRepo::getInstance
            )
        )
    }
}