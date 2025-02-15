package com.kylecorry.trail_sense.shared.extensions

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
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

fun PixelCoordinate.isSamePixel(other: PixelCoordinate): Boolean {
    // If either contains a NaN, return false
    if (x.isNaN() || y.isNaN() || other.x.isNaN() || other.y.isNaN()) {
        return false
    }

    return x.safeRoundToInt() == other.x.safeRoundToInt() && y.safeRoundToInt() == other.y.safeRoundToInt()
}

fun PixelCoordinate.squaredDistanceTo(other: PixelCoordinate): Float {
    return square(x - other.x) + square(y - other.y)
}

fun Path.drawLines(lines: FloatArray) {
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
        lineTo(x2, y2)
        lastX = x2
        lastY = y2
        i += 4
    }
}

inline fun tryStartForegroundOrNotify(context: Context, action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (e is ForegroundServiceStartNotAllowedException || e is SecurityException)) {
            ServiceRestartAlerter(context.applicationContext).alert()
            Log.d("tryStartForegroundOrNotify", "Cannot start service")
        } else {
            throw e
        }
    }
}