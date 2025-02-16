package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import androidx.annotation.IdRes
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters

data class SurvivalGuideSearchResult(
    val chapter: Chapter,
    val score: Float,
    val headingIndex: Int,
    val heading: String,
    val snippet: String
)

class SurvivalGuideFuzzySearch(private val context: Context) {

    private val additionalContractions = mapOf(
        "saltwater" to listOf("salt", "water"),
        "fishhook" to listOf("fish", "hook"),
        "fishhooks" to listOf("fish", "hooks"),
        "firestarter" to listOf("fire", "starter"),
        "firestarters" to listOf("fire", "starters")
    )

    private val additionalStemWords = mapOf(
        "knives" to "knife"
    )

    // These are words which the user may type as two words or hyphenated words
    private val preservedWords: Set<String> = setOf(
        "a-frame",
        "bowel movement",
        "bowel movements",
        "head ache",
        "head aches"
    )

    // These are words which have nearly the same meaning when searched
    private val synonyms: List<Set<String>> = listOf(
        // MEDICAL
        // Headache
        setOf(
            // Preserved
            "head ache",
            "head aches",
            // Stemmed (so misspellings are expected)
            "headach",
            "migrain",
            "concuss"
        ),
        // Defecation
        setOf(
            // Preserved
            "bowel movement",
            "bowel movements",
            // Stemmed (so misspellings are expected)
            "poop",
            "defec", // Defecate
            "diahrrea",
            "bowel",
            "fece" // Feces
        ),
        // Urination
        setOf(
            // Stemmed (so misspellings are expected)
            "urin", // Urine
            "pee"
        ),
        // Bathroom
        setOf(
            "bathroom",
            "restroom",
            "toilet",
            "latrin", // Latrine
            "outhous", // Outhouse
            "cathol", // Cathole
            "bidet",
        ),
        // Fractures (not just medical)
        setOf(
            // Stemmed (so misspellings are expected)
            "fractur",
            "broken",
            "broke"
        )
    )

    private val chapters = Chapters.getChapters(context)

    suspend fun search(
        query: String,
        @IdRes guideId: Int? = null
    ): List<SurvivalGuideSearchResult> {
        val chapter = chapters.firstOrNull { it.resource == guideId }
        val results = if (chapter != null) {
            searchChapter(query, chapter)
        } else {
            chapters.flatMap {
                searchChapter(query, it)
            }
        }
            .groupBy { it.chapter to it.headingIndex }
            .mapNotNull { (_, results) ->
                val bestResultSnippet = results
                    .maxBy { it.score }.snippet
                    .trim()
                    .trimStart('#', '-')
                    .trim()
                SurvivalGuideSearchResult(
                    results.first().chapter,
                    results.maxOf { it.score },
                    results.first().headingIndex,
                    results.first().heading.trimStart('#').trim(),
                    bestResultSnippet
                )
            }
            .sortedByDescending { it.score }

        val maxScore = results.maxOfOrNull { it.score } ?: 0f

        return results.filter { it.score >= maxScore * 0.5 }
    }

    private suspend fun searchChapter(
        query: String,
        chapter: Chapter
    ): List<SurvivalGuideSearchResult> {
        // TODO: Other languages?
        val content = onIO {
            TextUtils.loadTextFromResources(context, chapter.resource)
        }

        val sections = TextUtils.groupSections(TextUtils.getSections(content), null).mapNotNull {
            val heading = it.firstOrNull() ?: return@mapNotNull null
            TextUtils.TextSection(
                heading.title,
                1,
                heading.content + "\n" + it.drop(1).joinToString("\n") { it.content })
        }

        val matches = mutableListOf<SurvivalGuideSearchResult>()

        val chapterMatch = TextUtils.getQueryMatchPercent(
            query,
            chapter.title,
            preservedWords = preservedWords,
            synonyms = synonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        for ((index, section) in sections.withIndex()) {
            val fullContent = chapter.title + "\n" + (section.title ?: "") + "\n" + section.content

            val titleMatch = TextUtils.getQueryMatchPercent(
                query,
                section.title ?: "",
                preservedWords = preservedWords,
                synonyms = synonyms,
                additionalContractions = additionalContractions,
                additionalStemWords = additionalStemWords
            )

            val sectionMatch = TextUtils.getQueryMatchPercent(
                query,
                fullContent,
                preservedWords = preservedWords,
                synonyms = synonyms,
                additionalContractions = additionalContractions,
                additionalStemWords = additionalStemWords
            )

            matches.add(
                SurvivalGuideSearchResult(
                    chapter,
                    sectionMatch + titleMatch + chapterMatch,
                    index,
                    section.title ?: context.getString(R.string.overview),
                    section.content
                )
            )

        }

        return matches
    }

}