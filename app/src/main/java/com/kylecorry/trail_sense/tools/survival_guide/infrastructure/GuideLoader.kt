package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters

data class GuideSection(
    val level: Int?,
    val chapter: Chapter,
    val title: String?,
    val keywords: Set<String>,
    val summary: String?,
    val content: String?,
    val subsections: List<GuideSection> = emptyList()
)

data class GuideDetails(val chapter: Chapter, val sections: List<GuideSection>)

class GuideLoader(private val context: Context) {

    suspend fun load(includeContent: Boolean = true): List<GuideDetails> = onIO {
        val runner = ParallelCoroutineRunner()
        runner.map(Chapters.getChapters(context)) { load(it, includeContent) }
    }

    suspend fun load(chapter: Chapter, includeContent: Boolean = true): GuideDetails = onIO {
        val text = TextUtils.loadTextFromResources(context, chapter.resource)
        val sections = TextUtils.groupSections(TextUtils.getSections(text), null)

        var guideSections = sections.map {
            val section = getSectionDetails(it, chapter)

            // Level 3 headers in content
            val subsections = TextUtils.groupSections(
                TextUtils.getSections(section.content ?: ""),
                3,
            ).mapNotNull { subsection ->
                val details = getSectionDetails(subsection, chapter)
                if (details.level != 3) {
                    return@mapNotNull null
                }
                details.copy(content = if (includeContent) details.content else null)
            }

            section.copy(
                content = if (includeContent) section.content else null,
                subsections = subsections
            )
        }

        // Distribute the keywords from the first section to the rest of the sections (assuming the first section is the overview)
        if (guideSections.firstOrNull()?.title == null) {
            val keywords = guideSections.firstOrNull()?.keywords ?: emptySet()
            guideSections = guideSections.map {
                it.copy(keywords = it.keywords + keywords)
            }
        }

        // Distribute keywords in section
        guideSections = guideSections.map { section ->
            val keywords = section.keywords
            val subsections = section.subsections.map {
                it.copy(keywords = it.keywords + keywords)
            }
            section.copy(
                keywords = keywords + section.subsections.flatMap { it.keywords },
                subsections = subsections
            )
        }

        GuideDetails(chapter, guideSections)
    }

    private fun getSectionDetails(
        sections: List<TextUtils.TextSection>,
        chapter: Chapter
    ): GuideSection {
        val first = sections.first()
        val content =
            "${first.content}\n${
                sections.drop(1).joinToString("\n") { it.toMarkdown(false, false) }
            }"
        val keywords = TextUtils.getMarkdownKeywords(content)
        val summary = TextUtils.getMarkdownSummary(content)
        return GuideSection(
            level = first.level,
            title = first.title,
            keywords = keywords,
            summary = summary,
            content = content,
            chapter = chapter
        )
    }

}