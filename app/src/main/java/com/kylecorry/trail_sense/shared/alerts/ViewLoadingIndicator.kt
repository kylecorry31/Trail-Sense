package com.kylecorry.trail_sense.shared.alerts

import android.view.View
import androidx.core.view.isVisible

class ViewLoadingIndicator(private val view: View, private val hideWhenLoading: Boolean = false) :
    ILoadingIndicator {
    override fun show() {
        view.isVisible = !hideWhenLoading
    }

    override fun hide() {
        view.isVisible = hideWhenLoading
    }
}