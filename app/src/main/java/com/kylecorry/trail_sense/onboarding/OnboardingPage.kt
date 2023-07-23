package com.kylecorry.trail_sense.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.R

data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val contents: Int,
    @DrawableRes val image: Int,
    @StringRes val nextButtonText: Int = R.string.continue_button
)
