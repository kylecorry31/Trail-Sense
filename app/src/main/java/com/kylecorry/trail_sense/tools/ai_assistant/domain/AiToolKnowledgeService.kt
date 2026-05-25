package com.kylecorry.trail_sense.tools.ai_assistant.domain

import android.content.Context
import androidx.annotation.RawRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSearch
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class AiToolKnowledgeService(private val context: Context) {

    private val toolSearch by lazy { ToolSearch(context) }
    private val entries by lazy {
        TextUtils.loadTextFromResources(context, R.raw.ai_tool_knowledge)
            .let { AiToolKnowledgeParser.parse(it) }
            .associateBy { it.toolId }
    }

    fun getPromptContext(
        question: String,
        preferredToolId: String? = null,
        limit: Int = 2
    ): String? {
        val tools = getMatchingTools(question, preferredToolId, limit)
        if (tools.isEmpty()) {
            return null
        }

        val sections = tools.mapNotNull { tool ->
            val entry = entries[tool.id] ?: return@mapNotNull null
            buildToolSection(tool, entry, question)
        }

        if (sections.isEmpty()) {
            return null
        }

        return sections.joinToString("\n\n").trimForPrompt(MAX_CONTEXT_CHARS)
    }

    private fun getMatchingTools(
        question: String,
        preferredToolId: String?,
        limit: Int
    ): List<Tool> {
        val preferred = preferredToolId?.let { findPreferredTool(it) }
        val rankedIds = AiToolKnowledgeMatcher.rank(
            question,
            entries.values,
            preferred?.id,
            limit
        )
        val knowledgeMatches = rankedIds.mapNotNull { Tools.getTool(context, it) }
        val searched = toolSearch.search(question)

        return (knowledgeMatches + searched)
            .distinctBy { it.id }
            .take(limit)
    }

    private fun findPreferredTool(preferredToolId: String): Tool? {
        val id = preferredToolId.toLongOrNull() ?: knownToolIds[preferredToolId.lowercase()]
        if (id != null) {
            return Tools.getTool(context, id)
        }

        val normalized = preferredToolId.normalizeId()
        return Tools.getTools(context).firstOrNull {
            it.name.normalizeId() == normalized
        }
    }

    private fun buildToolSection(tool: Tool, entry: AiToolKnowledgeEntry, question: String): String {
        val guideExcerpt = tool.guideId?.let { loadGuideExcerpt(it, question) }

        return buildString {
            append("Tool: ")
            append(tool.name)
            append("\nNeeds: ")
            append(entry.needs)
            append("\nWhere: ")
            append(entry.where)
            append("\nHow: ")
            append(entry.how)
            append("\nValues: ")
            append(entry.values)
            append("\nCaveats: ")
            append(entry.caveats)
            entry.related?.let {
                append("\nRelated: ")
                append(it)
            }
            if (!guideExcerpt.isNullOrBlank()) {
                append("\nGuide excerpt:\n")
                append(guideExcerpt)
            }
        }.trimForPrompt(MAX_TOOL_CONTEXT_CHARS)
    }

    private fun loadGuideExcerpt(@RawRes guideId: Int, question: String): String? {
        return try {
            val guide = TextUtils.loadTextFromResources(context, guideId)
            AiGuideExcerptExtractor.extract(question, guide, MAX_GUIDE_EXCERPT_CHARS)
        } catch (_: Exception) {
            null
        }
    }

    private fun String.normalizeId(): String {
        return lowercase().filter { it.isLetterOrDigit() }
    }

    private fun String.trimForPrompt(maxChars: Int): String {
        if (length <= maxChars) {
            return this
        }
        return take(maxChars).trimEnd() + "..."
    }

    companion object {
        private const val MAX_CONTEXT_CHARS = 1800
        private const val MAX_TOOL_CONTEXT_CHARS = 1000
        private const val MAX_GUIDE_EXCERPT_CHARS = 800

        private val knownToolIds = mapOf(
            "weather" to Tools.WEATHER,
            "clouds" to Tools.CLOUDS,
            "navigation" to Tools.NAVIGATION
        )
    }
}
