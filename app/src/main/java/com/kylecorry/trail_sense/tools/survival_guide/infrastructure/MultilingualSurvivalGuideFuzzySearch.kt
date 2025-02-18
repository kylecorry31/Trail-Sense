package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.text.LevenshteinDistance
import com.kylecorry.trail_sense.shared.text.SimpleWordTokenizer

class MultilingualSurvivalGuideFuzzySearch(context: Context) : BaseSurvivalGuideSearch(context) {

    private val tokenizer = SimpleWordTokenizer()

    override fun searchChapter(
        query: String,
        guide: GuideDetails
    ): List<SurvivalGuideSearchResult> {
        val queryKeywords = tokenizer.tokenize(query).map { it.lowercase() }.toSet()

        return guide.sections.mapIndexed { index, section ->
            val textKeywords = section.keywords.flatMap { it.split("-") }.toSet()
            val headerKeywords =
                section.title?.let { tokenizer.tokenize(it).map { it.lowercase() }.toSet() }
                    ?: emptySet()
            val sectionScore = percentMatch(queryKeywords, textKeywords)
            val headerScore = percentMatch(queryKeywords, headerKeywords)

            val score = if (headerScore == 1f) {
                1.1f
            } else {
                maxOf(sectionScore, headerScore)
            }

            SurvivalGuideSearchResult(
                guide.chapter,
                score,
                index,
                section.title,
                section.summary
            )
        }
    }

    private fun percentMatch(queryKeywords: Set<String>, textKeywords: Set<String>): Float {
        val distanceMetric = LevenshteinDistance()
        val scores = mutableMapOf<String, Float>()

        for (qWord in queryKeywords) {
            if (qWord in textKeywords) {
                scores[qWord] = 1f
                continue
            }

            for (lWord in textKeywords) {
                val distance = distanceMetric.percentSimilarity(qWord, lWord)
                if (qWord !in scores) {
                    scores[qWord] = distance
                } else {
                    scores[qWord] = maxOf(scores[qWord] ?: 0f, distance)
                }
            }
        }

        var total = 0f
        for (word in queryKeywords) {
            if (word in scores) {
                total += scores[word] ?: 0f
            }
        }

        return total / queryKeywords.size
    }

}
