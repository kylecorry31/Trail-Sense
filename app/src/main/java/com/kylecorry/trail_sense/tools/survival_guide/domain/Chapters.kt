package com.kylecorry.trail_sense.tools.survival_guide.domain

import android.content.Context
import com.kylecorry.trail_sense.R

object Chapters {

    fun getChapters(context: Context): List<Chapter> {
        return listOf(
            Chapter(
                context.getString(R.string.overview),
                R.raw.guide_survival_chapter_overview,
                R.drawable.ic_open_book
            ),
            Chapter(
                context.getString(R.string.category_medical),
                R.raw.guide_survival_chapter_medical,
                R.drawable.ic_category_medical
            ),
            Chapter(
                context.getString(R.string.shelter),
                R.raw.guide_survival_chapter_shelter,
                R.drawable.ic_category_shelter
            ),
            Chapter(
                context.getString(R.string.water),
                R.raw.guide_survival_chapter_water,
                R.drawable.ic_category_water
            ),
            Chapter(
                context.getString(R.string.category_fire),
                R.raw.guide_survival_chapter_fire,
                R.drawable.ic_category_fire
            ),
            Chapter(
                context.getString(R.string.category_food),
                R.raw.guide_survival_chapter_food,
                R.drawable.ic_category_food
            ),
            Chapter(
                context.getString(R.string.navigation),
                R.raw.guide_survival_chapter_navigation,
                R.drawable.ic_category_navigation
            ),
            Chapter(
                context.getString(R.string.weather),
                R.raw.guide_survival_chapter_weather,
                R.drawable.cloud
            ),
        )
    }

}
