package com.kylecorry.trail_sense.tools.survival_guide.domain

import android.content.Context
import com.kylecorry.trail_sense.R

object Chapters {

    fun getChapters(context: Context): List<Chapter> {
        return listOf(
            Chapter(
                context.getString(R.string.overview),
                context.getString(R.string.chapter_number, 1),
                R.raw.guide_survival_chapter_overview,
                R.drawable.ic_user_guide
            ),
            Chapter(
                context.getString(R.string.category_medical),
                context.getString(R.string.chapter_number, 2),
                R.raw.guide_survival_chapter_medical,
                R.drawable.ic_category_medical
            ),
            Chapter(
                context.getString(R.string.water),
                context.getString(R.string.chapter_number, 3),
                R.raw.guide_survival_chapter_water,
                R.drawable.ic_category_water
            ),
            Chapter(
                context.getString(R.string.category_food),
                context.getString(R.string.chapter_number, 4),
                R.raw.guide_survival_chapter_food,
                R.drawable.ic_category_food
            ),
            Chapter(
                context.getString(R.string.category_fire),
                context.getString(R.string.chapter_number, 5),
                R.raw.guide_survival_chapter_fire,
                R.drawable.ic_category_fire
            ),
            Chapter(
                context.getString(R.string.shelter_and_clothing),
                context.getString(R.string.chapter_number, 6),
                R.raw.guide_survival_chapter_shelter_and_clothing,
                R.drawable.ic_category_shelter
            ),
            Chapter(
                context.getString(R.string.navigation),
                context.getString(R.string.chapter_number, 7),
                R.raw.guide_survival_chapter_navigation,
                R.drawable.ic_category_navigation
            ),
            Chapter(
                context.getString(R.string.tools),
                context.getString(R.string.chapter_number, 8),
                R.raw.guide_survival_chapter_tools,
                R.drawable.ic_axe
            ),
            Chapter(
                context.getString(R.string.knots_and_cordage),
                context.getString(R.string.chapter_number, 9),
                R.raw.guide_survival_knots_and_cordage,
                R.drawable.ic_knots
            ),
        )
    }

}