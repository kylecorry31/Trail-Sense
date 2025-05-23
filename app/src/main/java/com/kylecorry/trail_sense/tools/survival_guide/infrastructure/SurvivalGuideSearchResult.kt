package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter

data class SurvivalGuideSearchResult(
    val chapter: Chapter,
    val score: Float,
    val headingIndex: Int,
    val heading: String?,
    val summary: String?,
    val bestSubsection: SurvivalGuideSubsectionSearchResult? = null
)

data class SurvivalGuideSubsectionSearchResult(
    val score: Float,
    val headingIndex: Int,
    val heading: String?,
    val summary: String?
)