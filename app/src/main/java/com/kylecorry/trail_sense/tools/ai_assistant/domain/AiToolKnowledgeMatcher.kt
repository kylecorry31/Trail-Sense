package com.kylecorry.trail_sense.tools.ai_assistant.domain

object AiToolKnowledgeMatcher {

    fun rank(
        question: String,
        entries: Collection<AiToolKnowledgeEntry>,
        preferredToolId: Long? = null,
        limit: Int = 2
    ): List<Long> {
        val scored = entries
            .mapNotNull { entry ->
                val score = score(question, entry)
                if (score <= 0f) {
                    null
                } else {
                    val boost = if (entry.toolId == preferredToolId) PREFERRED_TOOL_BOOST else 0f
                    entry.toolId to score + boost
                }
            }
            .sortedByDescending { it.second }
            .map { it.first }

        return scored.take(limit)
    }

    fun isRelevant(question: String, entry: AiToolKnowledgeEntry): Boolean {
        return score(question, entry) > 0f
    }

    private fun score(question: String, entry: AiToolKnowledgeEntry): Float {
        val searchable = listOf(
            entry.needs,
            entry.where,
            entry.how,
            entry.values,
            entry.caveats,
            entry.related.orEmpty()
        ).joinToString(" ")

        val phraseScore = phraseScore(question, searchable)
        val tokenScore = tokenScore(question, searchable)
        return phraseScore + tokenScore
    }

    private fun phraseScore(question: String, searchable: String): Float {
        val normalizedQuestion = question.lowercase()
        val terms = searchable
            .split(',', ';', '，', '；', '|')
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 }

        return terms.count {
            normalizedQuestion.contains(it) || (it.any(::isCjk) && it.contains(normalizedQuestion))
        }.toFloat()
    }

    private fun tokenScore(question: String, searchable: String): Float {
        val queryTokens = tokenize(question)
        if (queryTokens.isEmpty()) {
            return 0f
        }

        val textTokens = tokenize(searchable)
        return queryTokens.count { it in textTokens }.toFloat() / queryTokens.size
    }

    private fun tokenize(text: String): Set<String> {
        val normalized = text.lowercase()
        val latinTokens = normalized
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }

        val cjkTokens = normalized
            .filter(::isCjk)
            .windowed(2)

        return (latinTokens + cjkTokens).toSet()
    }

    private fun isCjk(char: Char): Boolean {
        return char.code in 0x4E00..0x9FFF
    }

    private const val PREFERRED_TOOL_BOOST = 0.25f
}
