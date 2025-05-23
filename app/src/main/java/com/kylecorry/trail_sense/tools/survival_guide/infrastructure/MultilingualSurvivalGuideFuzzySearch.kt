package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.text.LevenshteinDistance
import com.kylecorry.trail_sense.shared.text.nlp.processors.LowercaseProcessor
import com.kylecorry.trail_sense.shared.text.nlp.processors.SequentialProcessor
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.PostProcessedTokenizer
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.SimpleWordTokenizer

class MultilingualSurvivalGuideFuzzySearch(context: Context) : BaseSurvivalGuideSearch(context) {

    private val tokenizer = PostProcessedTokenizer(
        SimpleWordTokenizer(),
        SequentialProcessor(
            LowercaseProcessor()
        )
    )

    override fun getSectionScore(query: String, section: GuideSection): Float {
        val queryKeywords = tokenizer.tokenize(query).toSet()

        val textKeywords = section.keywords.flatMap { it.split("-") }.toSet()
        val headerKeywords =
            section.title?.let { tokenizer.tokenize(it).toSet() }
                ?: emptySet()
        val sectionScore = percentMatch(queryKeywords, textKeywords)
        val headerScore = percentMatch(queryKeywords, headerKeywords)

        return if (headerScore == 1f) {
            1.1f
        } else {
            maxOf(sectionScore, headerScore)
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
