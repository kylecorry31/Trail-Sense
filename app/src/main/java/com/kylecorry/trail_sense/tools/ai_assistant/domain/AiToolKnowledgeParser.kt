package com.kylecorry.trail_sense.tools.ai_assistant.domain

object AiToolKnowledgeParser {

    fun parse(text: String): List<AiToolKnowledgeEntry> {
        return text
            .split(Regex("(?m)^##\\s+"))
            .drop(1)
            .mapNotNull { parseBlock(it) }
    }

    private fun parseBlock(block: String): AiToolKnowledgeEntry? {
        val fields = block.lines()
            .mapNotNull { line ->
                val index = line.indexOf(':')
                if (index == -1) {
                    null
                } else {
                    line.substring(0, index).trim().lowercase() to line.substring(index + 1).trim()
                }
            }
            .toMap()

        val toolId = fields["tool id"]?.toLongOrNull() ?: return null
        val needs = fields["needs"]?.takeIf { it.isNotBlank() } ?: return null
        val where = fields["where"]?.takeIf { it.isNotBlank() } ?: return null
        val how = fields["how"]?.takeIf { it.isNotBlank() } ?: return null
        val values = fields["values"]?.takeIf { it.isNotBlank() } ?: return null
        val caveats = fields["caveats"]?.takeIf { it.isNotBlank() } ?: return null

        return AiToolKnowledgeEntry(
            toolId = toolId,
            needs = needs,
            where = where,
            how = how,
            values = values,
            caveats = caveats,
            related = fields["related"]?.takeIf { it.isNotBlank() }
        )
    }
}
