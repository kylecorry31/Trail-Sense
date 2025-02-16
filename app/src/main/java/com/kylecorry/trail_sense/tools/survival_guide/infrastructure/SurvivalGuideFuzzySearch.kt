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

    private val preservedWords: Set<String> = setOf(
        "a-frame"
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
        // Load the guide text
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

        for ((index, section) in sections.withIndex()) {
            val titleMatch =
                TextUtils.getQueryMatchPercent(
                    query,
                    section.title ?: "",
                    preservedWords = preservedWords,
                    additionalContractions = additionalContractions,
                    additionalStemWords = additionalStemWords
                )
            val sectionMatches =
                TextUtils.fuzzySearch(
                    query,
                    section.content,
                    preservedWords = preservedWords,
                    additionalContractions = additionalContractions,
                    additionalStemWords = additionalStemWords
                )

            for (match in sectionMatches) {
                val content = section.content.substring(
                    match.second.start,
                    match.second.end.coerceAtMost(section.content.length)
                )

                // If the content is a markdown image, continue
                if (content.trim().startsWith("![") || content.trim().startsWith("[]")) {
                    continue
                }

                matches.add(
                    SurvivalGuideSearchResult(
                        chapter,
                        match.first + titleMatch,
                        index,
                        section.title ?: context.getString(R.string.overview),
                        content
                    )
                )
            }

            if (titleMatch > 0.5f && sectionMatches.isEmpty()) {
                matches.add(
                    SurvivalGuideSearchResult(
                        chapter,
                        titleMatch,
                        0,
                        section.title ?: context.getString(R.string.overview),
                        ""
                    )
                )
            }

        }

        return matches
    }

}