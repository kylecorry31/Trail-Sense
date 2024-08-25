package com.kylecorry.trail_sense.tools.survival_guide.domain

import android.content.Context
import com.kylecorry.trail_sense.R

object Chapters {

    fun getChapters(context: Context): List<Chapter> {
        return listOf(
            Chapter("Overview", "Chapter 1", R.raw.guide_survival_chapter_1),
            Chapter("Survival medicine", "Chapter 2", R.raw.guide_survival_chapter_2),
            Chapter("Water", "Chapter 3", R.raw.guide_survival_chapter_3),
            Chapter("Food", "Chapter 4", R.raw.guide_survival_chapter_4),
            Chapter("Fire", "Chapter 5", R.raw.guide_survival_chapter_5),
            Chapter("Shelter and clothing", "Chapter 6", R.raw.guide_survival_chapter_6),
            Chapter("Movement and navigation", "Chapter 7", R.raw.guide_survival_chapter_7),
            Chapter("Survival equipment", "Chapter 8", R.raw.guide_survival_chapter_8),
            Chapter("Survival knots and rope", "Appendix A", R.raw.guide_survival_appendix_a)
        )
    }

}