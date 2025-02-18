package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

interface SurvivalGuideSearch {
    suspend fun search(query: String): List<SurvivalGuideSearchResult>
}