package com.kylecorry.trail_sense.onboarding

import com.kylecorry.trail_sense.R

object OnboardingPages {

    const val EXPLORE = 0
    const val USER_GUIDE = 1
    const val PRIVACY = 2

    val pages = listOf(
        OnboardingPage(R.string.explore, R.string.onboarding_explore, R.drawable.steps),
        OnboardingPage(R.string.tool_user_guide_title, R.string.onboarding_user_guide, R.drawable.ic_user_guide),
        OnboardingPage(R.string.privacy_and_location, R.string.onboarding_privacy, R.drawable.ic_not_visible),
    )

}