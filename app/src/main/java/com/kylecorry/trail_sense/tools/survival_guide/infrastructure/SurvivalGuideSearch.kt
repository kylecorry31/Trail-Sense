package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources

class SurvivalGuideSearch(context: Context) {

    private val MIN_SUBSECTION_SCORE = 0.6f

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
        val contents = loader.load(result.chapter, true)
        val section = contents.sections[result.headingIndex]

        val builder = StringBuilder()
        if (result.bestSubsection != null && result.bestSubsection.score >= MIN_SUBSECTION_SCORE) {
            val subsection = section.subsections[result.bestSubsection.headingIndex]
            builder.append("### ${result.bestSubsection.heading?.uppercase()}\n")
            if (result.bestSubsection.summary != null) {
                builder.append("> ${result.bestSubsection.summary}")
            }
            builder.append("\n\n")
            builder.append(subsection.content)
        } else {
            if (result.summary != null) {
                builder.append("> ${result.summary}")
            }
            builder.append("\n\n")
            builder.append(section.content)
        }

        return builder.toString().trim()
    }
}