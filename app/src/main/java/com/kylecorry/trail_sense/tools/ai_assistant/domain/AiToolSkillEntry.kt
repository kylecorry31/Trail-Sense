package com.kylecorry.trail_sense.tools.ai_assistant.domain

data class AiToolSkillEntry(
    val id: String,
    val name: String,
    val needs: String,
    val summary: String,
    val toolIds: List<Long>,
    val steps: String,
    val interpretation: String? = null,
    val caveats: String,
    val samplePrompts: List<String>,
    val localizedName: String? = null,
    val localizedSummary: String? = null,
    val localizedSteps: String? = null,
    val localizedInterpretation: String? = null,
    val localizedCaveats: String? = null
) {
    fun name(language: String): String {
        return if (language == "zh") localizedName ?: name else name
    }

    fun summary(language: String): String {
        return if (language == "zh") localizedSummary ?: summary else summary
    }

    fun steps(language: String): String {
        return if (language == "zh") localizedSteps ?: steps else steps
    }

    fun interpretation(language: String): String? {
        return if (language == "zh") localizedInterpretation ?: interpretation else interpretation
    }

    fun caveats(language: String): String {
        return if (language == "zh") localizedCaveats ?: caveats else caveats
    }
}
