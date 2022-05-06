package com.kylecorry.trail_sense.shared.alerts

import android.view.View
import androidx.core.view.isVisible

class ViewLoadingIndicator(private val view: View) : ILoadingIndicator {
    override fun show() {
        view.isVisible = true
    }

    override fun hide() {
        view.isVisible = false
    }
}