package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.luna.cache.MemoryCachedValue

abstract class BaseSurvivalGuideSearch(protected val context: Context) : SurvivalGuideSearch {

    private val cache = MemoryCachedValue<List<GuideDetails>>()

    abstract fun searchChapter(query: String, guide: GuideDetails): List<SurvivalGuideSearchResult>

    override suspend fun search(query: String): List<SurvivalGuideSearchResult> {
        val results = getGuides().flatMap {
            searchChapter(query, it)
        }.sortedByDescending { it.score }

        val maxScore = results.firstOrNull()?.score ?: 0f

        return results.filter { it.score > 0f && it.score >= maxScore * 0.8 }
    }

    private suspend fun getGuides(): List<GuideDetails> {
        return cache.getOrPut {
            GuideLoader(context).load(false)
        }
    }
}