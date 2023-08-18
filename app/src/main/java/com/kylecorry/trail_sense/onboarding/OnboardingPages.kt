package com.kylecorry.trail_sense.onboarding

import com.kylecorry.trail_sense.R

object OnboardingPages {

    const val EXPLORE = 0
    const val USER_GUIDE = 1
    const val PRIVACY = 2
    const val DISCLAIMER = 3
    const val MISSING_COMPASS = 4

    val pages = listOf(
        OnboardingPage(R.string.explore, R.string.onboarding_explore, R.drawable.steps),
        OnboardingPage(
            R.string.tool_user_guide_title,
            R.string.onboarding_user_guide,
            R.drawable.ic_user_guide
        ),
        OnboardingPage(
            R.string.privacy_and_location,
            R.string.onboarding_privacy,
            R.drawable.ic_not_visible
        ),
        OnboardingPage(
            R.string.app_disclaimer_message_title,
            R.string.disclaimer_message_content,
            R.drawable.ic_tool_notes,
            R.string.i_agree
        ),
        OnboardingPage(
            R.string.no_compass_message,
            R.string.no_compass_description,
            R.drawable.ic_compass_icon
        )
    )

}