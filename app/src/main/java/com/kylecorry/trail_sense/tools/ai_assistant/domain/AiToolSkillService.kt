package com.kylecorry.trail_sense.tools.ai_assistant.domain

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class AiToolSkillService(private val context: Context) {

    private val entries by lazy {
        TextUtils.loadTextFromResources(context, R.raw.ai_tool_skills)
            .let { AiToolSkillParser.parse(it) }
    }

    fun getPromptContext(
        question: String,
        limit: Int = 1,
        enabledSkillIds: Set<String>? = null
    ): String? {
        val matches = getMatchingSkills(question, limit, enabledSkillIds)
        if (matches.isEmpty()) {
            return null
        }

        return matches.joinToString("\n\n") { buildSkillSection(it) }
            .trimForPrompt(MAX_CONTEXT_CHARS)
    }

    fun getMatchingToolIds(
        question: String,
        limit: Int = 1,
        enabledSkillIds: Set<String>? = null
    ): List<Long> {
        return getMatchingSkills(question, limit, enabledSkillIds)
            .flatMap { it.toolIds }
            .distinct()
    }

    fun getSkills(): List<AiToolSkillEntry> {
        return entries
    }

    fun getSamplePrompts(limit: Int = 4): List<String> {
        return entries
            .flatMap { it.samplePrompts }
            .distinct()
            .take(limit)
    }

    fun getMatchingSkills(
        question: String,
        limit: Int = 1,
        enabledSkillIds: Set<String>? = null
    ): List<AiToolSkillEntry> {
        val enabledEntries = if (enabledSkillIds == null) {
            entries
        } else {
            entries.filter { it.id in enabledSkillIds }
        }
        return AiToolSkillMatcher.rankWithScores(question, enabledEntries, limit)
            .filter { it.score >= MIN_MATCH_SCORE }
            .map { it.skill }
    }

    private fun buildSkillSection(entry: AiToolSkillEntry): String {
        val language = context.resources.configuration.locales[0]?.language ?: "en"
        val tools = entry.toolIds
            .mapNotNull { Tools.getTool(context, it)?.name }
            .joinToString(", ")

        return buildString {
            append("Skill workflow: ")
            append(entry.name(language))
            append("\nSummary: ")
            append(entry.summary(language))
            append("\nRecommended tools: ")
            append(tools)
            append("\nAnswer requirements: Answer in the user's language. Explain where to open each tool, how to use it, what readings or observations increase concern, what cannot be determined by Trail Sense, and what conservative next action to take. Do not only list tools.")
            append("\nSteps: ")
            append(entry.steps(language))
            entry.interpretation(language)?.let {
                append("\nHow to interpret the evidence: ")
                append(it)
            }
            append("\nCaveats: ")
            append(entry.caveats(language))
        }
    }

    private fun String.trimForPrompt(maxChars: Int): String {
        if (length <= maxChars) {
            return this
        }
        return take(maxChars).trimEnd() + "..."
    }

    companion object {
        private const val MAX_CONTEXT_CHARS = 3200
        private const val MIN_MATCH_SCORE = 0.35f
    }
}
