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
    val content: String?
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
                "${first.content}\n${it.drop(1).joinToString("\n") { it.toMarkdown(true) }}"
            val keywords = TextUtils.getMarkdownKeywords(content)
            val summary = TextUtils.getMarkdownSummary(content)
            GuideSection(first.title, keywords, summary, if (includeContent) content else null)
        }

        // Distribute the keywords from the first section to the rest of the sections (assuming the first section is the overview)
        if (guideSections.firstOrNull()?.title == null) {
            val keywords = guideSections.firstOrNull()?.keywords ?: emptySet()
            guideSections = guideSections.map {
                GuideSection(it.title, keywords + it.keywords, it.summary, it.content)
            }
        }

        GuideDetails(chapter, guideSections)
    }

}