package com.kylecorry.trail_sense.tools.ai_assistant.domain

object AiToolSkillMatcher {

    data class ScoredSkill(
        val skill: AiToolSkillEntry,
        val score: Float
    )

    fun rank(
        question: String,
        entries: Collection<AiToolSkillEntry>,
        limit: Int = 2
    ): List<AiToolSkillEntry> {
        return rankWithScores(question, entries, limit).map { it.skill }
    }

    fun rankWithScores(
        question: String,
        entries: Collection<AiToolSkillEntry>,
        limit: Int = 2
    ): List<ScoredSkill> {
        return entries
            .mapNotNull { entry ->
                val score = score(question, entry)
                if (score <= 0f) {
                    null
                } else {
                    ScoredSkill(entry, score)
                }
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun score(question: String, entry: AiToolSkillEntry): Float {
        val searchable = listOf(
            entry.name,
            entry.needs,
            entry.summary,
            entry.steps,
            entry.caveats,
            entry.samplePrompts.joinToString(" ")
        ).joinToString(" ")

        return phraseScore(question, searchable) + tokenScore(question, searchable)
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
}
