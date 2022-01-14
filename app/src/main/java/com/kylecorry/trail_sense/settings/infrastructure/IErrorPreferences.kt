package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.shared.ErrorBannerReason

interface IErrorPreferences {

    fun canShowError(error: ErrorBannerReason): Boolean
    fun setCanShowError(error: ErrorBannerReason, canShow: Boolean)

}