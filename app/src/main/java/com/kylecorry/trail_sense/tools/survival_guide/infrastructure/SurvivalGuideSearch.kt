package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources

class SurvivalGuideSearch(context: Context) {

    private val loader = GuideLoader(context)
    private val strategy = if (Resources.getLocale(context).language.startsWith("en")) {
        EnglishSurvivalGuideFuzzySearch(context, loader)
    } else {
        MultilingualSurvivalGuideFuzzySearch(loader)
    }

    suspend fun search(query: String): List<SurvivalGuideSearchResult> {
        return strategy.search(query)
    }

    suspend fun getSummary(query: String, result: SurvivalGuideSearchResult): String {
        val contents = loader.load(result.chapter, true)
        val section = contents.sections[result.headingIndex]

        val builder = StringBuilder()
        if (shouldUseSubsection(result)) {
            val subsection = section.subsections[result.bestSubsection!!.headingIndex]
            builder.append(subsection.content)
        } else {
            builder.append(section.content)
        }

        return builder.toString().trim()
    }

    companion object {
        private val MIN_SUBSECTION_SCORE = 0.6f

        fun shouldUseSubsection(result: SurvivalGuideSearchResult): Boolean {
            return result.bestSubsection != null && result.bestSubsection.score >= MIN_SUBSECTION_SCORE
        }

    }

}