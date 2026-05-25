package com.kylecorry.trail_sense.tools.ai_assistant.domain

object AiGuideExcerptExtractor {

    fun extract(query: String, guide: String, maxChars: Int = 800): String {
        val sections = parseSections(guide)
        if (sections.isEmpty()) {
            return guide.trimForPrompt(maxChars)
        }

        val overview = sections.firstOrNull { it.title == null }?.content.orEmpty()
        val bestSection = sections
            .filter { it.title != null }
            .maxByOrNull { score(query, "${it.title}\n${it.content}") }
            ?.takeIf { score(query, "${it.title}\n${it.content}") > 0f }

        val excerpt = buildString {
            if (overview.isNotBlank()) {
                append(overview.trim())
            }
            if (bestSection != null) {
                if (isNotBlank()) {
                    append("\n\n")
                }
                append("## ")
                append(bestSection.title)
                append("\n")
                append(bestSection.content.trim())
            }
        }

        return excerpt.trimForPrompt(maxChars)
    }

    private fun parseSections(guide: String): List<Section> {
        val sections = mutableListOf<Section>()
        var title: String? = null
        val content = StringBuilder()

        for (line in guide.lines()) {
            if (line.startsWith("## ")) {
                sections.add(Section(title, content.toString().trim()))
                title = line.removePrefix("## ").trim()
                content.clear()
            } else {
                content.appendLine(line)
            }
        }
        sections.add(Section(title, content.toString().trim()))

        return sections.filter { it.title != null || it.content.isNotBlank() }
    }

    private fun score(query: String, text: String): Float {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) {
            return 0f
        }

        val textTokens = tokenize(text)
        return queryTokens.count { it in textTokens }.toFloat() / queryTokens.size
    }

    private fun tokenize(text: String): Set<String> {
        return text
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }
            .toSet()
    }

    private fun String.trimForPrompt(maxChars: Int): String {
        val normalized = trim().replace(Regex("\\n{3,}"), "\n\n")
        if (normalized.length <= maxChars) {
            return normalized
        }
        return normalized.take(maxChars).trimEnd() + "..."
    }

    private data class Section(
        val title: String?,
        val content: String
    )
}
