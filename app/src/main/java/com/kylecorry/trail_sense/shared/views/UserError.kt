package com.kylecorry.trail_sense.shared.views

import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.shared.ErrorBannerReason

data class UserError(val reason: ErrorBannerReason, val title: String, @DrawableRes val icon: Int, val action: String? = null, val onAction: () -> Unit = {})
