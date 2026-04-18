package com.kylecorry.trail_sense.shared.text.search

import com.kylecorry.trail_sense.shared.text.LevenshteinDistance
import com.kylecorry.trail_sense.shared.text.nlp.processors.LowercaseProcessor
import com.kylecorry.trail_sense.shared.text.nlp.processors.SequentialProcessor
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.PostProcessedTokenizer
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.SimpleWordTokenizer
import kotlin.math.max

class MultilingualFuzzySearchStrategy(
    private val parentMatchBoost: Float = 1.15f,
    private val goodMatchThreshold: Float = 0.8f,
    private val titleMatchBoost: Float = 1.1f
) : SearchStrategy {

    private val tokenizer = PostProcessedTokenizer(
        SimpleWordTokenizer(),
        SequentialProcessor(
            LowercaseProcessor()
        )
    )

    override fun getSearchScore(
        query: String,
        item: SearchItem
    ): Float {
        val queryKeywords = tokenizer.tokenize(query).toSet()

        val textKeywords = item.keywords.flatMap { it.split("-") }.toSet()
        val headerKeywords = tokenizer.tokenize(item.title).toSet()
        val chapterKeywords = tokenizer.tokenize(item.parent?.title ?: "").toSet()
        var itemMatch = percentMatch(queryKeywords, textKeywords)
        var titleMatch = percentMatch(queryKeywords, headerKeywords)
        val inverseTitleMatch = percentMatch(headerKeywords, queryKeywords)
        val parentMatch = percentMatch(queryKeywords, chapterKeywords)

        if (parentMatch > goodMatchThreshold) {
            // If the chapter matches, boost the section match a little
            itemMatch *= parentMatchBoost
        }

        // If the user exactly matched the header, they probably want to see that
        if (titleMatch == 1f && inverseTitleMatch == 1f) {
            titleMatch = titleMatchBoost
        }

        return max(itemMatch, titleMatch)
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
