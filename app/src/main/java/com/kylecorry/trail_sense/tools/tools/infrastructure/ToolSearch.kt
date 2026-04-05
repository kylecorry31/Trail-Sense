package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.text.nlp.processors.LowercaseProcessor
import com.kylecorry.trail_sense.shared.text.nlp.processors.SequentialProcessor
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.PostProcessedTokenizer
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.SimpleWordTokenizer
import com.kylecorry.trail_sense.shared.text.search.EnglishFuzzySearchStrategy
import com.kylecorry.trail_sense.shared.text.search.MultilingualFuzzySearchStrategy
import com.kylecorry.trail_sense.shared.text.search.SearchItem
import com.kylecorry.trail_sense.shared.text.search.TitleScoreBoostSearchStrategy

class ToolSearch(context: Context) {

    private val tools = Tools.getTools(context)
    private val keywords = ToolKeywordLoader.load(context)
    private val strategy = TitleScoreBoostSearchStrategy(
        if (Resources.getLocale(context).language.startsWith("en")) {
            EnglishFuzzySearchStrategy()
        } else {
            MultilingualFuzzySearchStrategy()
        }
    )

    private val tokenizer = PostProcessedTokenizer(
        SimpleWordTokenizer(),
        SequentialProcessor(
            LowercaseProcessor()
        )
    )

    fun search(query: String): List<Tool> {
        if (query.isBlank()) return emptyList()

        val scored = tools.mapNotNull { tool ->
            val toolKeywords = (keywords[tool.id] ?: emptySet()) + tokenizer.tokenize(tool.name)
            val item = SearchItem(
                tool.id.toString(),
                tool.name,
                toolKeywords
            )
            val score = strategy.getSearchScore(query, item)
            if (score > 0f) tool to score else null
        }.sortedByDescending { it.second }

        val maxScore = scored.firstOrNull()?.second ?: 0f
        return scored
            .filter { it.second >= maxScore * 0.8f }
            .map { it.first }
    }
}
