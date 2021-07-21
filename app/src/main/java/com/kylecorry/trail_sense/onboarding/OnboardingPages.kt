package com.kylecorry.trail_sense.onboarding

import com.kylecorry.trail_sense.R

object OnboardingPages {

    const val NAVIGATION = 0
    const val WEATHER = 1
    const val ASTRONOMY = 2
    const val TOOLS = 3
    const val USER_GUIDE = 4
    const val PRIVACY = 5

    val pages = listOf(
        OnboardingPage(R.string.navigation, R.string.onboarding_navigation, R.drawable.ic_compass_icon),
        OnboardingPage(R.string.weather, R.string.onboarding_weather, R.drawable.ic_weather),
        OnboardingPage(R.string.astronomy, R.string.onboarding_astronomy, R.drawable.ic_astronomy),
        OnboardingPage(R.string.tools, R.string.onboarding_tools, R.drawable.inclinometer),
        OnboardingPage(R.string.tool_user_guide_title, R.string.onboarding_user_guide, R.drawable.ic_user_guide),
        OnboardingPage(R.string.privacy_and_location, R.string.onboarding_privacy, R.drawable.ic_not_visible),
    )

}