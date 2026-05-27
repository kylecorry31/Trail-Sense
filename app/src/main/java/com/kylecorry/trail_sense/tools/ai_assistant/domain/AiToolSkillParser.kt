package com.kylecorry.trail_sense.tools.ai_assistant.domain

object AiToolSkillParser {

    fun parse(text: String): List<AiToolSkillEntry> {
        return text
            .split(Regex("(?m)^##\\s+"))
            .drop(1)
            .mapNotNull { parseBlock(it) }
    }

    private fun parseBlock(block: String): AiToolSkillEntry? {
        val title = block.lineSequence().firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: return null
        val fields = block.lines()
            .drop(1)
            .mapNotNull { line ->
                val index = line.indexOf(':')
                if (index == -1) {
                    null
                } else {
                    line.substring(0, index).trim().lowercase() to line.substring(index + 1).trim()
                }
            }
            .toMap()

        val id = fields["skill id"]?.takeIf { it.isNotBlank() } ?: return null
        val needs = fields["needs"]?.takeIf { it.isNotBlank() } ?: return null
        val summary = fields["summary"]?.takeIf { it.isNotBlank() } ?: return null
        val toolIds = fields["tools"]
            ?.split(',', '，')
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val steps = fields["steps"]?.takeIf { it.isNotBlank() } ?: return null
        val caveats = fields["caveats"]?.takeIf { it.isNotBlank() } ?: return null
        val samplePrompts = fields["sample prompts"]
            ?.split('|')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        return AiToolSkillEntry(
            id = id,
            name = title,
            needs = needs,
            summary = summary,
            toolIds = toolIds,
            steps = steps,
            interpretation = fields["interpretation"]?.takeIf { it.isNotBlank() },
            caveats = caveats,
            samplePrompts = samplePrompts,
            localizedName = fields["name zh"]?.takeIf { it.isNotBlank() },
            localizedSummary = fields["summary zh"]?.takeIf { it.isNotBlank() },
            localizedSteps = fields["steps zh"]?.takeIf { it.isNotBlank() },
            localizedInterpretation = fields["interpretation zh"]?.takeIf { it.isNotBlank() },
            localizedCaveats = fields["caveats zh"]?.takeIf { it.isNotBlank() }
        )
    }
}
