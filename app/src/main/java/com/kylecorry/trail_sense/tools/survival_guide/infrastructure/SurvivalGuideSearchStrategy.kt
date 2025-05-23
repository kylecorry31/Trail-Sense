package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

interface SurvivalGuideSearchStrategy {
    suspend fun search(query: String): List<SurvivalGuideSearchResult>
}