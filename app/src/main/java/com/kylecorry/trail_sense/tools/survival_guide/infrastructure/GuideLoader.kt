package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters

data class GuideSection(
    val title: String?,
    val keywords: Set<String>,
    val summary: String?,
    val content: String?,
    val subsections: List<GuideSection> = emptyList()
)

data class GuideDetails(val chapter: Chapter, val sections: List<GuideSection>)

class GuideLoader(private val context: Context) {

    suspend fun load(includeContent: Boolean = true): List<GuideDetails> = onIO {
        Chapters.getChapters(context).map { load(it, includeContent) }
    }

    suspend fun load(chapter: Chapter, includeContent: Boolean = true): GuideDetails = onIO {
        val text = TextUtils.loadTextFromResources(context, chapter.resource)
        val sections = TextUtils.groupSections(TextUtils.getSections(text), null)

        var guideSections = sections.map {
            val first = it.first()
            val content =
                "${first.content}\n${it.drop(1).joinToString("\n") { it.toMarkdown(false, false) }}"
            val keywords = TextUtils.getMarkdownKeywords(content)
            val summary = TextUtils.getMarkdownSummary(content)

            // Level 3 headers in content
            val subsections = TextUtils.groupSections(
                TextUtils.getSections(content),
                3,
            ).mapNotNull { subsection ->
                val subsectionFirst = subsection.first()
                if (subsectionFirst.level != 3){
                    return@mapNotNull null
                }
                val subsectionContent =
                    "${subsectionFirst.content}\n${subsection.drop(1).joinToString("\n") { it.toMarkdown(false, false) }}"
                val subsectionKeywords = TextUtils.getMarkdownKeywords(subsectionContent)
                val subsectionSummary = TextUtils.getMarkdownSummary(subsectionContent)
                GuideSection(
                    title = subsectionFirst.title,
                    keywords = subsectionKeywords,
                    summary = subsectionSummary,
                    content = if (includeContent) subsectionContent else null
                )
            }

            GuideSection(first.title, keywords, summary, if (includeContent) content else null, subsections)
        }

        // Distribute the keywords from the first section to the rest of the sections (assuming the first section is the overview)
        if (guideSections.firstOrNull()?.title == null) {
            val keywords = guideSections.firstOrNull()?.keywords ?: emptySet()
            guideSections = guideSections.map {
                GuideSection(it.title, keywords + it.keywords, it.summary, it.content, it.subsections)
            }
        }

        GuideDetails(chapter, guideSections)
    }

}