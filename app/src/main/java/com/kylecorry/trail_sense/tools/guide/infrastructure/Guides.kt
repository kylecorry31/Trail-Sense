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

        val survival = listOf(
            UserGuideCategory(
                context.getString(R.string.survival_manual),
                listOf(
                    UserGuide(
                        "Chapter 1: Overview",
                        null,
                        R.raw.guide_survival_chapter_1
                    ),
                    UserGuide(
                        "Chapter 2: Survival medicine",
                        null,
                        R.raw.guide_survival_chapter_2
                    ),
                    UserGuide(
                        "Chapter 3: Water",
                        null,
                        R.raw.guide_survival_chapter_3
                    ),
                    UserGuide(
                        "Chapter 4: Food",
                        null,
                        R.raw.guide_survival_chapter_4
                    ),
                    UserGuide(
                        "Chapter 5: Fire",
                        null,
                        R.raw.guide_survival_chapter_5
                    ),
                    UserGuide(
                        "Chapter 6: Shelter and clothing",
                        null,
                        R.raw.guide_survival_chapter_6
                    ),
                    UserGuide(
                        "Chapter 7: Movement and navigation",
                        null,
                        R.raw.guide_survival_chapter_7
                    ),
                    UserGuide(
                        "Chapter 8: Survival equipment",
                        null,
                        R.raw.guide_survival_chapter_8
                    ),
                    UserGuide(
                        "Appendix A",
                        null,
                        R.raw.guide_survival_appendix_a
                    )
                )
            )
        )

        return general + toolGuides + survival
    }
}