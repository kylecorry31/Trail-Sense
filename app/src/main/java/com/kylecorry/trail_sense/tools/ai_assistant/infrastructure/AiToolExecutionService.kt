package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiSkillRun
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolCallCard
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunResult
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunStatus
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolSkillEntry
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolSkillMatcher
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolSkillService
import java.util.Locale

class AiToolExecutionService private constructor(
    private val skillProvider: () -> List<AiToolSkillEntry>,
    private val runner: AiTrailSenseToolRunner,
    private val languageProvider: () -> String
) {

    private var enabledSkillIds: Set<String>? = null
    private var activeSkill: AiToolSkillEntry? = null

    constructor(context: Context) : this(
        skillProvider = {
            AiToolSkillService(context.applicationContext).getSkills()
        },
        runner = TrailSenseAiToolRunner(context.applicationContext),
        languageProvider = {
            context.resources.configuration.locales[0]?.language ?: Locale.ENGLISH.language
        }
    )

    internal constructor(
        skills: List<AiToolSkillEntry>,
        runner: AiTrailSenseToolRunner,
        language: String = Locale.ENGLISH.language
    ) : this({ skills }, runner, { language })

    fun configure(enabledSkillIds: Set<String>?) {
        this.enabledSkillIds = enabledSkillIds
    }

    fun selectSkill(
        question: String,
        enabledSkillIds: Set<String>? = this.enabledSkillIds
    ): AiToolSkillEntry? {
        val skills = filteredSkills(enabledSkillIds)
        return AiToolSkillMatcher.rank(question, skills, limit = 1).firstOrNull()
    }

    fun activateSkill(skillNameOrId: String): AiToolSkillEntry? {
        val normalized = skillNameOrId.trim()
        val skill = filteredSkills(enabledSkillIds).firstOrNull {
            it.id.equals(normalized, ignoreCase = true) ||
                it.name.equals(normalized, ignoreCase = true) ||
                it.localizedName?.equals(normalized, ignoreCase = true) == true
        }
        activeSkill = skill
        return skill
    }

    fun activateSkill(skill: AiToolSkillEntry?) {
        activeSkill = skill
    }

    fun clearActiveSkill() {
        activeSkill = null
    }

    fun getRunningCards(skill: AiToolSkillEntry): List<AiToolCallCard> {
        return skill.toolIds.distinct().map { toolId ->
            AiToolCallCard(
                toolId = toolId,
                toolName = runner.getToolName(toolId),
                skillId = skill.id,
                status = AiToolRunStatus.Running,
                summary = "Reading current ${runner.getToolName(toolId)} data...",
                openedNavAction = runner.getOpenToolAction(toolId)
            )
        }
    }

    suspend fun execute(
        question: String,
        enabledSkillIds: Set<String>? = this.enabledSkillIds
    ): AiSkillRun? {
        val skill = selectSkill(question, enabledSkillIds) ?: return null
        return execute(skill)
    }

    suspend fun execute(skill: AiToolSkillEntry): AiSkillRun {
        activeSkill = skill
        val results = skill.toolIds.distinct().map { toolId ->
            runTool(toolId)
        }

        return AiSkillRun(
            skillId = skill.id,
            skillName = skill.name(languageProvider()),
            results = results,
            interpretationPrompt = buildInterpretationPrompt(skill)
        )
    }

    suspend fun runTool(toolId: Long, argumentsJson: String = "{}"): AiToolRunResult {
        val skill = activeSkill
        if (skill != null && toolId !in skill.toolIds) {
            return AiToolRunResult(
                toolId = toolId,
                toolName = runner.getToolName(toolId),
                status = AiToolRunStatus.Unavailable,
                summary = "${runner.getToolName(toolId)} is not part of the active skill workflow.",
                error = "Tool $toolId is outside the active skill allowlist.",
                openedNavAction = runner.getOpenToolAction(toolId)
            )
        }

        return runner.run(toolId, argumentsJson)
    }

    suspend fun runTool(toolIdText: String, argumentsJson: String = "{}"): AiToolRunResult {
        val toolId = toolIdText.trim().toLongOrNull()
            ?: return AiToolRunResult(
                toolId = 0,
                toolName = "Unknown tool",
                status = AiToolRunStatus.Failed,
                summary = "Invalid tool id: $toolIdText",
                error = "Tool id must be numeric."
            )
        return runTool(toolId, argumentsJson)
    }

    private fun filteredSkills(enabledSkillIds: Set<String>?): List<AiToolSkillEntry> {
        val skills = skillProvider()
        if (enabledSkillIds == null) {
            return skills
        }
        return skills.filter { it.id in enabledSkillIds }
    }

    private fun buildInterpretationPrompt(skill: AiToolSkillEntry): String {
        val language = languageProvider()
        return buildString {
            append(skill.interpretation(language) ?: skill.steps(language))
            append(" ")
            append(skill.caveats(language))
            append(" Base judgments only on succeeded or unavailable tool results. ")
            append("Never treat an unavailable tool as a measured reading. ")
            append("For safety-critical workflows, use higher concern, lower concern, or insufficient evidence language; never give a safe or unsafe guarantee.")
        }
    }
}
