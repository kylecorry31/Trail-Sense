package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import com.kylecorry.luna.cache.MemoryCachedValue
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner

abstract class BaseSurvivalGuideSearch(private val loader: GuideLoader) :
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
            val subsectionScores =
                section.subsections.filter { it.keywords.any() }.mapIndexed { i, subsection ->
                    Triple(i, subsection, getSectionScore(query, subsection))
                }.sortedByDescending { it.third }

            val bestSubsections =
                subsectionScores.filter { it.third == subsectionScores.firstOrNull()?.third }

            // If there are multiple subsections with the same score, then there is no best subsection
            val bestSubsection = bestSubsections.singleOrNull()

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

        val runner = ParallelCoroutineRunner()
        val results = runner.map(getGuides()) {
            searchChapter(query, it)
        }.flatten().sortedByDescending { it.score }

        val maxScore = results.firstOrNull()?.score ?: 0f

        return results.filter { it.score > 0f && it.score >= maxScore * 0.8 }
    }

    private suspend fun getGuides(): List<GuideDetails> {
        return cache.getOrPut {
            loader.load(false)
        }
    }
}