package com.kylecorry.trail_sense.shared.text.search

import com.kylecorry.trail_sense.shared.text.TextUtils
import kotlin.math.max

class EnglishFuzzySearchStrategy(
    private val preservedWords: Set<String> = emptySet(),
    private val additionalStopWords: Set<String> = emptySet(),
    private val synonyms: List<Set<String>> = emptyList(),
    private val additionalContractions: Map<String, List<String>> = emptyMap(),
    private val additionalStemWords: Map<String, String> = emptyMap(),
    private val parentMatchBoost: Float = 1.15f,
    private val goodMatchThreshold: Float = 0.8f,
    private val titleMatchBoost: Float = 1.1f
) : SearchStrategy {
    override fun getSearchScore(query: String, item: SearchItem): Float {
        val sectionKeywords = item.keywords.joinToString(", ")

        val additionalPreservedWords =
            item.keywords.filter { it.contains(" ") || it.contains("-") }.toMutableSet()

        // Any keywords with a dash should have a synonym with a space
        val additionalSynonyms = item.keywords
            .filter { it.contains("-") }
            .map { setOf(it, it.replace("-", " ")) }

        // Add the synonyms to the preserved words
        additionalPreservedWords.addAll(additionalSynonyms.flatten())

        var itemMatch = TextUtils.getQueryMatchPercent(
            query,
            sectionKeywords,
            preservedWords = preservedWords + additionalPreservedWords,
            additionalStopWords = additionalStopWords,
            synonyms = synonyms + additionalSynonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        var titleMatch = TextUtils.getQueryMatchPercent(
            query,
            item.title,
            preservedWords = preservedWords + additionalPreservedWords,
            additionalStopWords = additionalStopWords,
            synonyms = synonyms + additionalSynonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        val inverseTitleMatch = TextUtils.getQueryMatchPercent(
            item.title,
            query,
            preservedWords = preservedWords + additionalPreservedWords,
            additionalStopWords = additionalStopWords,
            synonyms = synonyms + additionalSynonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        val parentMatch = item.parent?.let {
            TextUtils.getQueryMatchPercent(
                query,
                item.parent.title,
                preservedWords = preservedWords + additionalPreservedWords,
                additionalStopWords = additionalStopWords,
                synonyms = synonyms + additionalSynonyms,
                additionalContractions = additionalContractions,
                additionalStemWords = additionalStemWords
            )
        }

        itemMatch *= item.scoreMultiplier

        if (parentMatch != null && parentMatch > goodMatchThreshold) {
            // If the parent matches, boost the item match a little
            itemMatch *= parentMatchBoost
        }

        // If the title has a good match, increase it
        if (titleMatch == 1f && inverseTitleMatch == 1f) {
            titleMatch = titleMatchBoost
        }

        return max(itemMatch, titleMatch)
    }
}