package com.kylecorry.trail_sense.shared.andromeda_temp

import android.content.Context
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.setMargins
import com.kylecorry.andromeda.alerts.Alerts

inline fun <T> Alerts.withProgress(
    context: Context,
    title: CharSequence,
    action: (setProgress: (Float) -> Unit) -> T
): T {
    val container = FrameLayout(context)
    val progressBar = ProgressBar(
        context,
        null,
        android.R.attr.progressBarStyleHorizontal
    ).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(32)
        }
        isIndeterminate = false
        max = 100
    }
    container.addView(progressBar)

    val progressDialog = Alerts.dialog(
        context,
        title,
        contentView = container,
        cancelable = false,
        cancelOnOutsideTouch = false,
        okText = null,
        cancelText = null
    )

    try {
        return action {
            progressBar.post {
                progressBar.progress = (it * 100).toInt()
            }
        }
    } finally {
        progressDialog.dismiss()
    }
}