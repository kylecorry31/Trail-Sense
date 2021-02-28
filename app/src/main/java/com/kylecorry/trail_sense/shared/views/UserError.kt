package com.kylecorry.trail_sense.shared.views

import androidx.annotation.DrawableRes

data class UserError(val title: String, @DrawableRes val icon: Int, val action: String? = null, val onAction: () -> Unit = {})
