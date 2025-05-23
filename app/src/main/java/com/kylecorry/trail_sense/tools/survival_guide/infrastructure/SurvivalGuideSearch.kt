package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources

class SurvivalGuideSearch(context: Context) {

    private val strategy = if (Resources.getLocale(context).language.startsWith("en")) {
        EnglishSurvivalGuideFuzzySearch(context)
    } else {
        MultilingualSurvivalGuideFuzzySearch(context)
    }

    private val loader = GuideLoader(context)

    suspend fun search(query: String): List<SurvivalGuideSearchResult> {
        return strategy.search(query)
    }

    suspend fun getSummary(query: String, result: SurvivalGuideSearchResult): String {
        // TODO: use the query to determine the subsection
        val contents = loader.load(result.chapter, true)
        val section = contents.sections[result.headingIndex]
        return section.content ?: result.summary ?: ""
    }
}