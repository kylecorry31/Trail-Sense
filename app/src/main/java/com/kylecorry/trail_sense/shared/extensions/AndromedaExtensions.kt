package com.kylecorry.trail_sense.shared.extensions

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources

inline fun Alerts.withCancelableLoading(
    context: Context,
    title: String,
    cancelText: CharSequence = context.getString(android.R.string.cancel),
    noinline onCancel: () -> Unit,
    action: () -> Unit
) {
    val loadingAlert = cancelableLoading(context, title, cancelText, onCancel)
    try {
        action()
    } finally {
        loadingAlert.dismiss()
    }
}


fun Alerts.cancelableLoading(
    context: Context,
    title: String,
    cancelText: CharSequence = context.getString(android.R.string.cancel),
    onCanceled: (() -> Unit)? = null
): AlertDialog {
    val view = FrameLayout(context)
    val params = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER
    )
    view.layoutParams = params
    val loading = CircularProgressIndicator(context)
    loading.isIndeterminate = true

    val loadingParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER
    )
    loadingParams.bottomMargin = Resources.dp(context, 16f).toInt()
    loadingParams.topMargin = Resources.dp(context, 16f).toInt()
    loading.layoutParams = loadingParams
    view.addView(loading)

    return dialog(
        context,
        title,
        contentView = view,
        okText = null,
        cancelText = cancelText,
        cancelOnOutsideTouch = false
    ) { cancelled ->
        if (cancelled) {
            onCanceled?.invoke()
        }
    }
}