package com.kylecorry.trail_sense.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.R

data class OnboardingPage(
    val title: String,
    val contents: CharSequence,
    @DrawableRes val image: Int,
    val nextButtonText: String? = null,
)
