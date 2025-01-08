package com.kylecorry.trail_sense.tools.tools.ui.sort

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory

class CategoricalToolSort(context: Context) : ToolSort {

    private val groupNameMap = mapOf(
        ToolCategory.Signaling to context.getString(R.string.tool_category_signaling),
        ToolCategory.Distance to context.getString(R.string.distance),
        ToolCategory.Location to context.getString(R.string.location),
        ToolCategory.Angles to context.getString(R.string.tool_category_angles),
        ToolCategory.Time to context.getString(R.string.time),
        ToolCategory.Power to context.getString(R.string.power),
        ToolCategory.Weather to context.getString(R.string.weather),
        ToolCategory.Communication to context.getString(R.string.communication),
        ToolCategory.Books to context.getString(R.string.books),
        ToolCategory.Other to context.getString(R.string.other)
    )

    override fun sort(tools: List<Tool>): List<CategorizedTools> {
        return tools.sortedBy { it.category.ordinal }.groupBy { it.category }
            .map { (category, tools) ->
                CategorizedTools(groupNameMap[category], tools.sortedBy { it.name })
            }
    }
}