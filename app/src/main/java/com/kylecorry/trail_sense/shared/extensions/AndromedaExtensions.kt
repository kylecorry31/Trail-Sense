package com.kylecorry.trail_sense.shared.extensions

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.google.android.material.loadingindicator.LoadingIndicator
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundToInt

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

inline fun Alerts.withCancelableProgress(
    context: Context,
    title: String,
    cancelText: CharSequence = context.getString(android.R.string.cancel),
    noinline onCancel: () -> Unit,
    action: ((Float) -> Unit) -> Unit
) {
    val (progressAlert, setProgress) = cancelableProgress(
        context,
        title,
        cancelText,
        onCancel
    )
    try {
        action(setProgress)
    } finally {
        progressAlert.dismiss()
    }
}

fun Alerts.cancelableProgress(
    context: Context,
    title: String,
    cancelText: CharSequence = context.getString(android.R.string.cancel),
    onCanceled: (() -> Unit)? = null
): Pair<AlertDialog, (Float) -> Unit> {
    val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
    }
    val margin = Resources.dp(context, 24f).toInt()
    val view = FrameLayout(context).apply {
        addView(
            progress,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = margin
                marginEnd = margin
                topMargin = margin
                bottomMargin = margin
            }
        )
    }
    val dialog = dialog(
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
    return dialog to { value ->
        progress.post {
            progress.progress = (value.coerceIn(0f, 1f) * progress.max).toInt()
        }
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
    val loading = LoadingIndicator(context)

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

fun PixelCoordinate.isSamePixel(other: PixelCoordinate): Boolean {
    // If either contains a NaN, return false
    if (x.isNaN() || y.isNaN() || other.x.isNaN() || other.y.isNaN()) {
        return false
    }

    return x.safeRoundToInt() == other.x.safeRoundToInt() && y.safeRoundToInt() == other.y.safeRoundToInt()
}

fun Path.drawLines(lines: FloatArray, smooth: Boolean = false) {
    // Lines are in the form [x1, y1, x2, y2, x3, y3, ...]
    // Where x1, y1 is the first point and x2, y2 is the second point of the line
    // There can be gaps in the lines
    var i = 0
    var lastX = 0f
    var lastY = 0f
    while (i < lines.size) {
        val x1 = lines[i]
        val y1 = lines[i + 1]
        val x2 = lines[i + 2]
        val y2 = lines[i + 3]
        if (x1 != lastX || y1 != lastY) {
            moveTo(x1, y1)
        }
        if (smooth) {
            val mx = (x1 + x2) / 2f
            val my = (y1 + y2) / 2f
            quadTo(x1, x2, mx, my)
        } else {
            lineTo(x2, y2)
        }
        lastX = x2
        lastY = y2
        i += 4
    }

    if (smooth && lines.size >= 4) {
        val x1 = lines[lines.size - 4]
        val y1 = lines[lines.size - 3]
        val x2 = lines[lines.size - 2]
        val y2 = lines[lines.size - 1]
        quadTo(x1, y1, x2, y2)
    }
}

inline fun tryStartForegroundOrNotify(context: Context, action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        if (isForegroundServiceStartError(e)) {
            ServiceRestartAlerter(context.applicationContext).alert()
            getAppService<Logger>().warn("ForegroundService", "Cannot start foreground service", e)
        } else {
            throw e
        }
    }
}

@Suppress("TooGenericExceptionCaught")
inline fun Service.tryStartForegroundOrNotify(action: () -> Int): Int {
    return try {
        action()
    } catch (e: Exception) {
        if (!isForegroundServiceStartError(e)) {
            throw e
        }
        getAppService<Logger>().warn("ForegroundService", "Cannot start ${javaClass.simpleName} in the foreground", e)
        ServiceRestartAlerter(this).alert()
        stopSelf()
        Service.START_NOT_STICKY
    }
}

fun isForegroundServiceStartError(error: Exception): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (error is ForegroundServiceStartNotAllowedException || error is SecurityException)
}
