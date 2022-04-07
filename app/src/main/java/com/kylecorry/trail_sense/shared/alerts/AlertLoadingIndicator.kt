package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.kylecorry.andromeda.alerts.Alerts

class AlertLoadingIndicator(private val context: Context, private val title: String) :
    ILoadingIndicator {

    private var dialog: AlertDialog? = null

    override fun show() {
        hide()
        dialog = Alerts.loading(context, title)
    }

    override fun hide() {
        dialog?.dismiss()
        dialog = null
    }
}