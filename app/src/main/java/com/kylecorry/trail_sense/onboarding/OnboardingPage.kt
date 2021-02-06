package com.kylecorry.trail_sense.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnboardingPage(@StringRes val title: Int, @StringRes val contents: Int, @DrawableRes val image: Int)
