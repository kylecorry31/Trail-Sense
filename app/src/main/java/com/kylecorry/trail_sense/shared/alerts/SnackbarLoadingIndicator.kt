package com.kylecorry.trail_sense.shared.alerts

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts

class SnackbarLoadingIndicator(
    private val fragment: Fragment,
    private val anchor: View,
    private val text: String
) :
    ILoadingIndicator {

    private var snackbar: Snackbar? = null

    override fun show() {
        hide()
        snackbar = Alerts.snackbar(fragment, anchor, text, duration = Snackbar.LENGTH_INDEFINITE)
    }

    override fun hide() {
        snackbar?.dismiss()
        snackbar = null
    }
}