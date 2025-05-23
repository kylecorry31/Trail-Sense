package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.luna.cache.MemoryCachedValue

abstract class BaseSurvivalGuideSearch(protected val context: Context) :
    SurvivalGuideSearchStrategy {

    private val cache = MemoryCachedValue<List<GuideDetails>>()

    abstract fun getSectionScore(
        query: String,
        section: GuideSection
    ): Float

    private fun searchChapter(
        query: String,
        guide: GuideDetails
    ): List<SurvivalGuideSearchResult> {
        // TODO: Other languages?

//        Log.d(
//            "SurvivalGuideFuzzySearch", TextUtils.getKeywords(
//                query,
//                preservedWords = preservedWords,
//                additionalStopWords = additionalStopWords,
//                additionalContractions = additionalContractions,
//                additionalStemWords = additionalStemWords
//            ).joinToString(", ")
//        )

        val sections = guide.sections

        val matches = mutableListOf<SurvivalGuideSearchResult>()

        for ((index, section) in sections.withIndex()) {
            val bestSubsection =
                section.subsections.filter { it.keywords.any() }.mapIndexed { i, subsection ->
                    Triple(i, subsection, getSectionScore(query, subsection))
                }.maxByOrNull { it.third }

            matches.add(
                SurvivalGuideSearchResult(
                    guide.chapter,
                    getSectionScore(query, section),
                    index,
                    section.title,
                    section.summary,
                    bestSubsection = bestSubsection?.let {
                        SurvivalGuideSubsectionSearchResult(
                            it.third,
                            it.first,
                            it.second.title,
                            it.second.summary
                        )
                    }
                )
            )
        }

        return matches
    }


    override suspend fun search(query: String): List<SurvivalGuideSearchResult> {
        val results = getGuides().flatMap {
            searchChapter(query, it)
        }.sortedByDescending { it.score }

        val maxScore = results.firstOrNull()?.score ?: 0f

        return results.filter { it.score > 0f && it.score >= maxScore * 0.8 }
    }

    private suspend fun getGuides(): List<GuideDetails> {
        return cache.getOrPut {
            GuideLoader(context).load(false)
        }
    }
}