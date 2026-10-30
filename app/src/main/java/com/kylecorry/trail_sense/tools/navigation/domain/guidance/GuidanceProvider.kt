package com.kylecorry.trail_sense.tools.navigation.domain.guidance

interface GuidanceProvider {
    fun getGuidance(input: GuidanceInput): Guidance
}
