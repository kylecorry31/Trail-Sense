package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.CategoricalToolSort

object Guides {

    fun guides(context: Context): List<UserGuideCategory> {
        val tools = Tools.getTools(context)
        val sortedTools = CategoricalToolSort(context).sort(tools)

        val otherCategoryName = context.getString(R.string.other)

        val toolGuides = sortedTools.mapNotNull { category ->
            val guides = category.tools.mapNotNull { tool ->
                if (tool.guideId == null) {
                    return@mapNotNull null
                }

                UserGuide(
                    tool.name,
                    tool.description,
                    tool.guideId
                )
            } + listOfNotNull(
                // Add recommended apps guide to the bottom of the other category
                if (category.categoryName == otherCategoryName) {
                    UserGuide(
                        context.getString(R.string.guide_recommended_apps),
                        context.getString(R.string.guide_recommended_apps_description),
                        R.raw.guide_tool_recommended_apps
                    )
                } else null
            )

            if (guides.isEmpty()) {
                return@mapNotNull null
            }

            UserGuideCategory(
                category.categoryName ?: context.getString(R.string.tools),
                guides
            )
        }
        val general = listOf(
            UserGuideCategory(
                context.getString(R.string.general),
                listOf(
                    UserGuide(
                        context.getString(R.string.tools),
                        null,
                        R.raw.guide_tool_tools
                    )
                )
            )
        )

        return general + toolGuides
    }
}