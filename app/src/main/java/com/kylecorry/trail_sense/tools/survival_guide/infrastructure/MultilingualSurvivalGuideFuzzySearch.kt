package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import com.kylecorry.trail_sense.shared.text.search.MultilingualFuzzySearchStrategy
import com.kylecorry.trail_sense.shared.text.search.SearchItem

class MultilingualSurvivalGuideFuzzySearch(loader: GuideLoader) : BaseSurvivalGuideSearch(loader) {

    private val search = MultilingualFuzzySearchStrategy()

    override fun getSectionScore(query: String, section: GuideSection): Float {
        val item = SearchItem(
            "${section.chapter.title} ${section.title}",
            section.title ?: "",
            section.keywords,
            parent = SearchItem(section.chapter.title, section.chapter.title)
        )
        return search.getSearchScore(query, item)
    }

}
